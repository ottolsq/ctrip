package com.ctrip.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ctrip.common.exception.AuthenticationException;
import com.ctrip.common.exception.BusinessException;
import com.ctrip.common.exception.DuplicateResourceException;
import com.ctrip.common.exception.TokenExpiredException;
import com.ctrip.user.dto.request.*;
import com.ctrip.user.dto.response.TokenResponse;
import com.ctrip.user.entity.PasswordResetToken;
import com.ctrip.user.entity.RefreshToken;
import com.ctrip.user.entity.User;
import com.ctrip.user.entity.enums.UserStatus;
import com.ctrip.user.mapper.PasswordResetTokenMapper;
import com.ctrip.user.mapper.RefreshTokenMapper;
import com.ctrip.user.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;

/**
 * 认证服务实现类，处理注册、登录、令牌管理和密码重置的完整业务逻辑。
 *
 * <h3>安全设计</h3>
 * <ul>
 *   <li>Refresh token：服务端只存 SHA-256 哈希，原始值仅通过响应体返回一次。</li>
 *   <li>Refresh token 轮换：每次刷新吊销旧 token 并颁发新 token，防止 token 重放。</li>
 *   <li>密码重置 OTP：SHA-256 哈希存库，有效期 10 分钟，使用后立即标记 used=true。</li>
 *   <li>防用户枚举：forgotPassword 不存在时静默返回，login 不区分"用户不存在"与"密码错误"。</li>
 * </ul>
 */
@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    /** 密码重置 OTP 有效期（分钟） */
    private static final int OTP_TTL_MINUTES = 10;

    private final UserMapper userMapper;
    private final RefreshTokenMapper refreshTokenMapper;
    private final PasswordResetTokenMapper passwordResetTokenMapper;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final SmsService smsService;
    private final EmailService emailService;

    public AuthServiceImpl(UserMapper userMapper,
                           RefreshTokenMapper refreshTokenMapper,
                           PasswordResetTokenMapper passwordResetTokenMapper,
                           JwtService jwtService,
                           PasswordEncoder passwordEncoder,
                           SmsService smsService,
                           EmailService emailService) {
        this.userMapper = userMapper;
        this.refreshTokenMapper = refreshTokenMapper;
        this.passwordResetTokenMapper = passwordResetTokenMapper;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.smsService = smsService;
        this.emailService = emailService;
    }

    // -----------------------------------------------------------------------
    // 公开接口实现
    // -----------------------------------------------------------------------

    @Override
    @Transactional
    public TokenResponse registerByPhone(RegisterByPhoneRequest request) {
        // 1. 校验短信验证码（校验成功后自动从缓存消费）
        smsService.validateVerificationCode(request.phone(), request.smsCode());

        // 2. 手机号唯一性校验
        if (existsByPhone(request.phone())) {
            throw new DuplicateResourceException("手机号已被注册");
        }
        // 3. 用户名唯一性校验
        if (existsByUsername(request.username())) {
            throw new DuplicateResourceException("用户名已被使用");
        }

        // 4. 创建用户：手机注册直接激活，phoneVerified=true
        User user = User.builder()
                .username(request.username())
                .phone(request.phone())
                .passwordHash(passwordEncoder.encode(request.password()))
                .status(UserStatus.ACTIVE)
                .phoneVerified(true)
                .emailVerified(false)
                .build();
        userMapper.insert(user);

        return generateTokenResponse(user);
    }

    @Override
    @Transactional
    public TokenResponse registerByEmail(RegisterByEmailRequest request) {
        // 1. 邮箱唯一性校验
        if (existsByEmail(request.email())) {
            throw new DuplicateResourceException("邮箱已被注册");
        }
        // 2. 用户名唯一性校验
        if (existsByUsername(request.username())) {
            throw new DuplicateResourceException("用户名已被使用");
        }

        // 3. 创建用户：邮件验证暂未实现，初始状态 UNVERIFIED
        User user = User.builder()
                .username(request.username())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .status(UserStatus.UNVERIFIED)
                .emailVerified(false)
                .phoneVerified(false)
                .build();
        userMapper.insert(user);

        return generateTokenResponse(user);
    }

    @Override
    @Transactional
    public TokenResponse login(LoginRequest request) {
        // 1. 按 credential 查找用户
        User user = findUserByCredential(request.credential());

        // 2. 用户不存在快速返回通用错误（防枚举）
        if (user == null) {
            throw new AuthenticationException("用户名或密码错误");
        }

        // 3. 封禁账号检查在 BCrypt 验证之前：确保无论密码是否正确，响应一致，
        //    避免通过"封禁"与"密码错误"两种不同响应暴露密码正确性（H-4）
        if (UserStatus.SUSPENDED == user.getStatus()) {
            throw new BusinessException("账号已被封禁，请联系客服");
        }

        // 4. BCrypt 密码验证（高耗时操作，放在快速检查之后）
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new AuthenticationException("用户名或密码错误");
        }

        // 5. 更新最后登录时间
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, user.getId())
                .set(User::getLastLoginAt, LocalDateTime.now()));

        return generateTokenResponse(user);
    }

    @Override
    @Transactional
    public TokenResponse refreshToken(RefreshTokenRequest request) {
        String tokenHash = sha256Hex(request.refreshToken());

        // 1. 按哈希查找 refresh token
        RefreshToken stored = refreshTokenMapper.selectOne(
                new LambdaQueryWrapper<RefreshToken>()
                        .eq(RefreshToken::getTokenHash, tokenHash));

        if (stored == null) {
            throw new TokenExpiredException("Refresh token 无效");
        }
        if (Boolean.TRUE.equals(stored.getRevoked())) {
            throw new TokenExpiredException("Refresh token 已吊销");
        }
        if (stored.getExpiresAt().isBefore(LocalDateTime.now())) {
            revokeRefreshToken(stored);
            throw new TokenExpiredException("Refresh token 已过期");
        }

        // 2. 吊销旧 token（轮换策略）
        revokeRefreshToken(stored);

        // 3. 查找关联用户并颁发新令牌对
        User user = userMapper.selectById(stored.getUserId());
        if (user == null) {
            throw new AuthenticationException("用户不存在");
        }
        return generateTokenResponse(user);
    }

    @Override
    @Transactional
    public void logout(String rawRefreshToken) {
        String tokenHash = sha256Hex(rawRefreshToken);
        RefreshToken stored = refreshTokenMapper.selectOne(
                new LambdaQueryWrapper<RefreshToken>()
                        .eq(RefreshToken::getTokenHash, tokenHash));
        // token 不存在或已吊销时静默处理（幂等）
        if (stored != null && !Boolean.TRUE.equals(stored.getRevoked())) {
            revokeRefreshToken(stored);
        }
    }

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        // 防用户枚举：用户不存在时静默返回
        User user = findUserByCredential(request.credential());
        if (user == null) {
            log.debug("[ForgotPassword] 用户不存在，静默返回 credential={}", request.credential());
            return;
        }

        String otp = generateOtp();
        String otpHash = sha256Hex(otp);
        boolean isEmail = request.credential().contains("@");

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .userId(user.getId())
                .tokenHash(otpHash)
                .channel(isEmail ? "EMAIL" : "SMS")
                .expiresAt(LocalDateTime.now().plusMinutes(OTP_TTL_MINUTES))
                .used(false)
                .build();
        passwordResetTokenMapper.insert(resetToken);

        if (isEmail) {
            emailService.sendPasswordResetCode(user.getEmail(), otp);
        } else {
            smsService.sendOtp(user.getPhone(), otp);
        }
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        User user = findUserByCredential(request.credential());
        // 防枚举：用户不存在与 OTP 无效返回相同错误，与 forgotPassword 的防枚举策略保持一致（H-6）
        if (user == null) {
            throw new BusinessException("OTP 无效或已过期");
        }

        String otpHash = sha256Hex(request.otp());

        // 查找有效且未使用的 OTP 令牌
        PasswordResetToken resetToken = passwordResetTokenMapper.selectOne(
                new LambdaQueryWrapper<PasswordResetToken>()
                        .eq(PasswordResetToken::getUserId, user.getId())
                        .eq(PasswordResetToken::getTokenHash, otpHash)
                        .eq(PasswordResetToken::getUsed, false)
                        .gt(PasswordResetToken::getExpiresAt, LocalDateTime.now())
                        .orderByDesc(PasswordResetToken::getExpiresAt)
                        .last("LIMIT 1"));

        if (resetToken == null) {
            throw new BusinessException("OTP 无效或已过期");
        }

        // 更新密码哈希
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, user.getId())
                .set(User::getPasswordHash, passwordEncoder.encode(request.newPassword())));

        // 标记 OTP 已使用（防止重放攻击）
        passwordResetTokenMapper.update(null, new LambdaUpdateWrapper<PasswordResetToken>()
                .eq(PasswordResetToken::getId, resetToken.getId())
                .set(PasswordResetToken::getUsed, true));

        // 密码变更后吊销该用户所有 refresh token，强制重新登录
        refreshTokenMapper.update(null, new LambdaUpdateWrapper<RefreshToken>()
                .eq(RefreshToken::getUserId, user.getId())
                .set(RefreshToken::getRevoked, true)
                .set(RefreshToken::getRevokedAt, LocalDateTime.now()));
    }

    @Override
    public void sendSmsCode(String phone) {
        smsService.sendVerificationCode(phone);
    }

    // -----------------------------------------------------------------------
    // 私有辅助方法
    // -----------------------------------------------------------------------

    /**
     * 生成令牌响应：同时颁发 access token 和 refresh token。
     *
     * <p>refresh token 原始值（64 字符 hex）仅在此处返回一次，服务端只存其 SHA-256 哈希。
     */
    private TokenResponse generateTokenResponse(User user) {
        byte[] raw = new byte[32];
        new SecureRandom().nextBytes(raw);
        String rawToken = HexFormat.of().formatHex(raw);
        String tokenHash = sha256Hex(rawToken);

        LocalDateTime expiresAt = jwtService.calculateRefreshTokenExpiry();

        RefreshToken refreshToken = RefreshToken.builder()
                .userId(user.getId())
                .tokenHash(tokenHash)
                .expiresAt(expiresAt)
                .revoked(false)
                .build();
        refreshTokenMapper.insert(refreshToken);

        String accessToken = jwtService.generateAccessToken(user.getId());
        long expiresIn = jwtService.getAccessTokenExpiresInSeconds();

        return new TokenResponse(accessToken, rawToken, expiresIn, "Bearer");
    }

    /** 吊销指定的 refresh token，设置 revoked=true 及吊销时间。 */
    private void revokeRefreshToken(RefreshToken token) {
        refreshTokenMapper.update(null, new LambdaUpdateWrapper<RefreshToken>()
                .eq(RefreshToken::getId, token.getId())
                .set(RefreshToken::getRevoked, true)
                .set(RefreshToken::getRevokedAt, LocalDateTime.now()));
    }

    /**
     * 根据 credential（邮箱或手机号）查找用户（排除已删除账号）。
     *
     * @param credential 邮箱（含 "@"）或手机号
     * @return 找到的用户，不存在时返回 {@code null}
     */
    private User findUserByCredential(String credential) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .ne(User::getStatus, UserStatus.DELETED);
        if (credential.contains("@")) {
            wrapper.eq(User::getEmail, credential);
        } else {
            wrapper.eq(User::getPhone, credential);
        }
        return userMapper.selectOne(wrapper);
    }

    private boolean existsByPhone(String phone) {
        return userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getPhone, phone)
                .ne(User::getStatus, UserStatus.DELETED)) > 0;
    }

    private boolean existsByEmail(String email) {
        return userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getEmail, email)
                .ne(User::getStatus, UserStatus.DELETED)) > 0;
    }

    private boolean existsByUsername(String username) {
        return userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)
                .ne(User::getStatus, UserStatus.DELETED)) > 0;
    }

    /** 生成 6 位随机数字 OTP（补零保证固定长度）。 */
    private String generateOtp() {
        return String.format("%06d", new SecureRandom().nextInt(1_000_000));
    }

    /**
     * 计算字符串的 SHA-256 哈希，返回十六进制字符串（64 字符）。
     *
     * @param value 待哈希的字符串（UTF-8 编码）
     * @return HEX 格式的 SHA-256 哈希值
     */
    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 算法不可用", e);
        }
    }
}
