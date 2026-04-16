# 捷程旅行网 后端架构设计文档

> 所有模块的设计方案、待实施计划均记录于此。
> 实施前先在此文档中写明方案，确认后再执行。

---

## 目录

- [技术栈](#技术栈)
- [用户模块](#用户模块)
  - [包结构](#包结构)
  - [数据库设计](#数据库设计)
  - [pom.xml 新增依赖](#pomxml-新增依赖)
  - [application.properties 配置](#applicationproperties-配置)
  - [API 端点](#api-端点)
  - [分层架构职责](#分层架构职责)
  - [DTO 定义](#dto-定义)
  - [安全设计](#安全设计)
  - [开发调试说明](#开发调试说明)
  - [实施顺序](#实施顺序)
  - [代码审查记录](#代码审查记录)

---

## 技术栈

| 层级 | 技术 | 版本 |
|------|------|------|
| 语言 | Java | 25 |
| 框架 | Spring Boot | 4.0.5 |
| Web 层 | Spring MVC | 随 Boot |
| 持久层 | MyBatis Plus | 3.5.x |
| 数据库 | MySQL | 8.1 |
| 安全 | Spring Security + JJWT | 随 Boot / 0.12.6 |
| 构建工具 | Maven Wrapper | 3.9.14 |

> **包命名说明：** groupId `com.ctrip`，artifactId `backend`，Java 根包为 `com.ctrip`（不使用 groupId+artifactId 拼接，避免 `com.ctrip.ctrip` 重复）。

---

## 用户模块

### 包结构

```
com.ctrip/   ← 根包（groupId=com.ctrip，artifactId=backend）
├── CtripApplication.java
│
├── common/
│   ├── exception/
│   │   ├── BusinessException.java
│   │   ├── ResourceNotFoundException.java
│   │   ├── AuthenticationException.java
│   │   ├── TokenExpiredException.java
│   │   └── DuplicateResourceException.java
│   ├── response/
│   │   └── ApiResponse.java                # 通用响应信封 record
│   ├── validation/
│   │   └── PhoneNumber.java                # 手机号自定义校验注解
│   └── web/
│       └── GlobalExceptionHandler.java     # @RestControllerAdvice
│
├── config/
│   ├── SecurityConfig.java
│   ├── JwtConfig.java                      # @ConfigurationProperties
│   ├── RateLimitConfig.java                # @ConfigurationProperties，限流参数
│   ├── WebConfig.java                      # @EnableConfigurationProperties + 注册 RateLimitInterceptor
│   └── MybatisPlusConfig.java              # MetaObjectHandler，处理 createdAt/updatedAt/issuedAt 自动填充
│
└── user/
    ├── controller/
    │   ├── AuthController.java             # /api/v1/auth/**
    │   └── UserController.java             # /api/v1/users/**
    ├── service/
    │   ├── AuthService.java
    │   ├── AuthServiceImpl.java
    │   ├── UserService.java
    │   ├── UserServiceImpl.java
    │   ├── JwtService.java
    │   ├── JwtServiceImpl.java
    │   ├── SmsService.java                 # 接口（对外发短信）
    │   ├── StubSmsServiceImpl.java         # 桩实现（@Profile("!prod")，开发/测试用）
    │   ├── EmailService.java               # 接口（对外发邮件）
    │   └── StubEmailServiceImpl.java       # 桩实现（@Profile("!prod")，开发/测试用）
    ├── mapper/                             # MyBatis Plus Mapper 接口
    │   ├── UserMapper.java                 # extends BaseMapper<User>
    │   ├── RefreshTokenMapper.java         # extends BaseMapper<RefreshToken>
    │   └── PasswordResetTokenMapper.java   # extends BaseMapper<PasswordResetToken>
    ├── entity/
    │   ├── User.java                       # @TableName("users")
    │   ├── RefreshToken.java               # @TableName("refresh_tokens")
    │   ├── PasswordResetToken.java         # @TableName("password_reset_tokens")
    │   └── enums/
    │       ├── UserStatus.java             # ACTIVE, SUSPENDED, UNVERIFIED, DELETED
    │       └── Gender.java                 # MALE, FEMALE, UNSPECIFIED
    ├── dto/
    │   ├── request/
    │   │   ├── RegisterByPhoneRequest.java
    │   │   ├── RegisterByEmailRequest.java
    │   │   ├── LoginRequest.java
    │   │   ├── RefreshTokenRequest.java
    │   │   ├── UpdateProfileRequest.java
    │   │   ├── ChangePasswordRequest.java
    │   │   ├── ForgotPasswordRequest.java
    │   │   └── ResetPasswordRequest.java
    │   └── response/
    │       ├── TokenResponse.java
    │       ├── UserProfileResponse.java
    │       └── MessageResponse.java
    ├── security/
    │   ├── JwtAuthenticationFilter.java    # OncePerRequestFilter
    │   ├── UserDetailsServiceImpl.java
    │   └── RateLimitInterceptor.java       # HandlerInterceptor，Bucket4j 令牌桶限流
    └── converter/
        └── UserConverter.java              # Entity ↔ DTO 静态转换方法
```

---

### 数据库设计

#### users 表

```sql
CREATE TABLE users (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    username        VARCHAR(50)     NOT NULL,
    email           VARCHAR(100)    DEFAULT NULL,
    phone           VARCHAR(20)     DEFAULT NULL,
    password_hash   VARCHAR(72)     NOT NULL  COMMENT 'BCrypt hash',
    avatar_url      VARCHAR(512)    DEFAULT NULL,
    gender          TINYINT         NOT NULL  DEFAULT 0  COMMENT '0=UNSPECIFIED 1=MALE 2=FEMALE',
    birthday        DATE            DEFAULT NULL,
    real_name       VARCHAR(50)     DEFAULT NULL  COMMENT '实名认证姓名',
    status          TINYINT         NOT NULL  DEFAULT 0  COMMENT '0=UNVERIFIED 1=ACTIVE 2=SUSPENDED 3=DELETED',
    email_verified  TINYINT(1)      NOT NULL  DEFAULT 0,
    phone_verified  TINYINT(1)      NOT NULL  DEFAULT 0,
    last_login_at   DATETIME        DEFAULT NULL,
    created_at      DATETIME        NOT NULL  DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL  DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT pk_users          PRIMARY KEY (id),
    CONSTRAINT uq_users_username UNIQUE (username),
    CONSTRAINT uq_users_email    UNIQUE (email),
    CONSTRAINT uq_users_phone    UNIQUE (phone)
);

CREATE INDEX idx_users_email  ON users (email);
CREATE INDEX idx_users_phone  ON users (phone);
CREATE INDEX idx_users_status ON users (status);
```

#### refresh_tokens 表

```sql
CREATE TABLE refresh_tokens (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    user_id     BIGINT       NOT NULL,
    token_hash  VARCHAR(64)  NOT NULL  COMMENT 'SHA-256 hex，不存原始值',
    device_info VARCHAR(255) DEFAULT NULL,
    issued_at   DATETIME     NOT NULL  DEFAULT CURRENT_TIMESTAMP,
    expires_at  DATETIME     NOT NULL,
    revoked     TINYINT(1)   NOT NULL  DEFAULT 0,
    revoked_at  DATETIME     DEFAULT NULL,

    CONSTRAINT pk_refresh_tokens PRIMARY KEY (id),
    CONSTRAINT uq_refresh_token  UNIQUE (token_hash),
    CONSTRAINT fk_rt_user_id     FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_rt_user_id    ON refresh_tokens (user_id);
CREATE INDEX idx_rt_token_hash ON refresh_tokens (token_hash);
CREATE INDEX idx_rt_expires_at ON refresh_tokens (expires_at);
```

#### password_reset_tokens 表

```sql
CREATE TABLE password_reset_tokens (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    user_id    BIGINT      NOT NULL,
    token_hash VARCHAR(64) NOT NULL  COMMENT 'SHA-256 of OTP',
    channel    VARCHAR(10) NOT NULL  COMMENT 'EMAIL or SMS',
    expires_at DATETIME    NOT NULL,
    used       TINYINT(1)  NOT NULL  DEFAULT 0,

    CONSTRAINT pk_prt            PRIMARY KEY (id),
    CONSTRAINT uq_prt_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_prt_user_id    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_prt_user_id ON password_reset_tokens (user_id);
```

---

### pom.xml 新增依赖

```xml
<!-- MyBatis Plus（Spring Boot 3/4 通用 starter） -->
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
    <version>3.5.12</version>
</dependency>

<!-- Spring Security -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>

<!-- Bean Validation -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>

<!-- JJWT 0.12.x -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.6</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>

<!-- Lombok（仅用于 @Entity，DTO 使用 record） -->
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <optional>true</optional>
</dependency>

<!-- Bucket4j 限流 -->
<dependency>
    <groupId>com.bucket4j</groupId>
    <artifactId>bucket4j-core</artifactId>
    <version>8.10.1</version>
</dependency>

<!-- 配置元数据处理器 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-configuration-processor</artifactId>
    <optional>true</optional>
</dependency>

<!-- Jackson：ObjectMapper 供 Filter / SecurityConfig / RateLimitInterceptor 使用
     spring-boot-starter-webmvc（SB 4.x）不自动引入 Jackson，需显式声明 -->
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
</dependency>

<!-- H2 内存数据库：仅用于 CI 单元测试，替换 MySQL 连接（test scope） -->
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>test</scope>
</dependency>
```

---

### application.properties 配置

```properties
spring.application.name=ctrip

# ===== 数据源 =====
# 开发阶段使用硬编码明文（见"数据库凭据 C-1"说明），生产环境须改为环境变量注入
spring.datasource.url=jdbc:mysql://localhost:3306/ctrip?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=UTF-8
spring.datasource.username=root
spring.datasource.password=123456
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# ===== MyBatis Plus =====
mybatis-plus.mapper-locations=classpath*:mapper/**/*.xml
mybatis-plus.type-aliases-package=com.ctrip.user.entity
# 枚举包扫描：让 MyBatis Plus 识别 @EnumValue 注解，自动完成枚举与数据库值的互转
mybatis-plus.type-enums-package=com.ctrip.user.entity.enums
# 开启驼峰映射：数据库 password_hash → Java passwordHash
mybatis-plus.configuration.map-underscore-to-camel-case=true
# 关闭 SQL 日志（生产环境），开发调试时可改为 org.apache.ibatis.logging.stdout.StdOutImpl
mybatis-plus.configuration.log-impl=org.apache.ibatis.logging.nologging.NoLoggingImpl
mybatis-plus.global-config.db-config.id-type=auto
# 注意：不使用 MyBatis Plus 逻辑删除插件，软删除通过 UserStatus.DELETED（status=3）实现

# ===== JWT =====
# 开发阶段使用硬编码测试密钥，生产环境须通过环境变量 JWT_SECRET 注入 256-bit 随机值
app.jwt.secret=VGhpcyBpcyBhIHNlY3VyZSByYW5kb20ga2V5IGZvciBKV1Qgc2lnbmluZw==
# access token 有效期：15 分钟（毫秒）
app.jwt.access-token-expiration-ms=900000
# refresh token 有效期：15 天（毫秒）
app.jwt.refresh-token-expiration-ms=1296000000
app.jwt.issuer=ctrip

# ===== 限流（Bucket4j，认证端点，按 IP 计算）=====
app.rate-limit.auth.capacity=10
app.rate-limit.auth.refill-tokens=10
app.rate-limit.auth.refill-duration-seconds=60
```

---

### API 端点

#### 认证端点（无需 JWT）— `AuthController`

| Method | Path | 状态码 | 说明 |
|--------|------|--------|------|
| POST | `/api/v1/auth/register/phone` | 201 | 手机号 + 短信验证码注册，成功返回令牌对 |
| POST | `/api/v1/auth/register/email` | 201 | 邮箱 + 密码注册，成功返回令牌对 |
| POST | `/api/v1/auth/login` | 200 | 登录（手机/邮箱 + 密码） |
| POST | `/api/v1/auth/refresh` | 200 | 刷新 access token（轮换 refresh token） |
| POST | `/api/v1/auth/logout` | 200 | 登出（吊销 refresh token，幂等） |
| POST | `/api/v1/auth/password/forgot` | 200 | 发起密码重置（防枚举：用户不存在时同样返回 200） |
| POST | `/api/v1/auth/password/reset` | 200 | 完成密码重置（OTP 验证 + 吊销所有 refresh token） |
| POST | `/api/v1/auth/sms/send` | 200 | 发送短信验证码（开发阶段通过日志输出） |

#### 用户端点（需 JWT）— `UserController`

| Method | Path | 状态码 | 说明 |
|--------|------|--------|------|
| GET | `/api/v1/users/me` | 200 | 查看个人资料 |
| PUT | `/api/v1/users/me` | 200 | 更新个人资料（username / avatarUrl / gender / birthday / realName） |
| PUT | `/api/v1/users/me/password` | 200 | 修改密码（需提供当前密码） |
| PUT | `/api/v1/users/me/avatar` | 200 | 更新头像 URL |

#### 统一响应格式

```json
// 成功
{ "success": true, "data": { ... }, "error": null }

// 失败
{ "success": false, "data": null, "error": "错误信息" }
```

---

### 分层架构职责

| 层 | 职责 |
|----|------|
| Controller | 解析请求、校验、调用 Service、包装 ApiResponse；禁止含业务逻辑 |
| Service | 业务编排、事务边界（`@Transactional`）、DTO ↔ Entity 转换 |
| Mapper | 数据访问（MyBatis Plus `BaseMapper<T>`）；只返回 Entity / 基本类型 |
| Entity | `@TableName` 映射；`@TableId`/`@TableField` 管理字段；不暴露给 Controller |
| DTO | `record` 类型；Request 含校验注解，Response 为只读视图 |
| Converter | 静态方法完成 Entity ↔ DTO 转换；无 Spring Bean，无状态 |
| Security | JWT 过滤、UserDetails 加载；不含业务逻辑 |

**核心规则：**
- 构造器注入，禁止字段 `@Autowired`
- 写操作加 `@Transactional`，读操作可省略（MyBatis 默认 auto-commit）
- Entity 不直接返回给 Controller，必须经 Converter 转 DTO
- Entity 用 Lombok `@Data` + `@Builder`，`@TableField(fill=...)` 处理自动填充

---

### DTO 定义

```java
// ===== Request =====

// 注：phone 字段使用项目自定义注解 @PhoneNumber（common/validation/PhoneNumber.java），
// 其内部正则与 @Pattern(regexp = "^\\+?[1-9]\\d{7,14}$") 完全等价
public record RegisterByPhoneRequest(
    @NotBlank @PhoneNumber String phone,
    @NotBlank @Size(min = 6, max = 6) String smsCode,
    @NotBlank @Size(min = 8, max = 64) String password,
    @NotBlank @Size(min = 2, max = 50) String username
) {}

public record RegisterByEmailRequest(
    @NotBlank @Email String email,
    @NotBlank @Size(min = 8, max = 64) String password,
    @NotBlank @Size(min = 2, max = 50) String username
) {}

public record LoginRequest(
    @NotBlank String credential,   // 邮箱或手机号
    @NotBlank String password
) {}

public record RefreshTokenRequest(@NotBlank String refreshToken) {}

public record UpdateProfileRequest(
    @Size(min = 2, max = 50) String username,
    @Size(max = 512) String avatarUrl,
    Gender gender,
    LocalDate birthday,
    @Size(max = 50) String realName
) {}

public record ChangePasswordRequest(
    @NotBlank String currentPassword,
    @NotBlank @Size(min = 8, max = 64) String newPassword
) {}

public record ForgotPasswordRequest(@NotBlank String credential) {}

public record SendSmsRequest(
    @NotBlank @PhoneNumber String phone
) {}

public record UpdateAvatarRequest(
    @NotBlank @Size(max = 512) String avatarUrl
) {}

public record ResetPasswordRequest(
    @NotBlank String credential,
    @NotBlank @Size(min = 6, max = 6) String otp,
    @NotBlank @Size(min = 8, max = 64) String newPassword
) {}

// ===== Response =====

public record TokenResponse(
    String accessToken,
    String refreshToken,
    long expiresIn,
    String tokenType
) {}

public record UserProfileResponse(
    Long id,
    String username,
    String email,
    String phone,
    String avatarUrl,
    String gender,
    LocalDate birthday,
    String realName,
    boolean emailVerified,
    boolean phoneVerified,
    String status,
    LocalDateTime createdAt
) {}

public record MessageResponse(String message) {}

// ===== 通用信封 =====

public record ApiResponse<T>(boolean success, T data, String error) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null);
    }
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, null, message);
    }
}
```

---

### 安全设计

#### 密码存储
- BCrypt，强度 12（`BCryptPasswordEncoder(12)`）
- 后续可在下次登录时透明迁移至 Argon2id

#### JWT
- Access Token：15 分钟有效期，携带 `userId`、`iss`
- Refresh Token：15 天有效期，不含敏感信息
- 库：JJWT 0.12.x（`Jwts.parser()`，`SecretKey` 类型）
- Secret 通过环境变量 `${JWT_SECRET}` 注入，256-bit Base64 编码随机值

#### Refresh Token 存储策略
1. 生成 256-bit 随机原始值（`SecureRandom`）
2. 计算 SHA-256 哈希存入 `refresh_tokens.token_hash`
3. 原始值只在当次响应中返回，不落库
4. 刷新时：客户端发送原始值 → 服务端哈希 → 按哈希查库 → 校验后轮换（旧行吊销，新行插入）

#### 限流（Bucket4j）
| 端点 | 限制 |
|------|------|
| POST `/auth/login` | 5次/分钟/IP |
| POST `/auth/register/**` | 3次/分钟/IP |
| POST `/auth/password/forgot` | 3次/分钟/IP |
| POST `/auth/sms/send` | 2次/分钟/IP |

#### Security 配置要点
- 禁用 CSRF（无状态 API）
- `SessionCreationPolicy.STATELESS`
- 白名单：`POST /api/v1/auth/**`
- 其余路径全部需要 JWT 认证
- 401/403 返回 JSON，不重定向

---

### 开发调试说明

> 当前处于**开发调试阶段**，所有外部消息通道（短信、邮件）均使用**桩实现（Stub）**，
> 不真正调用第三方服务，OTP / 验证码仅通过 `log.debug` 打印到应用日志，
> 方便本地调试时直接查看。

#### 数据库凭据（C-1）

开发阶段 `application.properties` 中数据库用户名/密码使用硬编码明文：

```properties
spring.datasource.username=root
spring.datasource.password=123456
```

这是**有意为之**的开发期便捷配置，降低本地搭建环境的门槛。  
**生产部署前必须**将其替换为环境变量注入（`${DB_USERNAME}` / `${DB_PASSWORD}`），
并在 CI/CD 流程中通过 Secrets Manager 或 Kubernetes Secret 注入，禁止将凭据提交到代码仓库。

#### 短信桩实现：`StubSmsServiceImpl`

| 方法 | 行为 |
|------|------|
| `sendVerificationCode(phone)` | 生成 6 位随机数字验证码，存入内存 `ConcurrentHashMap`（10 分钟 TTL），并以 `log.debug` 打印：`[StubSms] 验证码已发送 phone=xxx code=xxxxxx` |
| `validateVerificationCode(phone, code)` | 校验内存缓存中的验证码，通过后立即消费（一次性）；不存在/过期/不匹配抛 `BusinessException` |
| `sendOtp(phone, otp)` | 仅打印 `log.debug [StubSms] OTP 已发送 phone=xxx otp=xxxxxx`，不真正发送短信 |

#### 邮件桩实现：`StubEmailServiceImpl`

| 方法 | 行为 |
|------|------|
| `sendPasswordResetCode(email, otp)` | 仅打印 `log.debug [StubEmail] 密码重置 OTP 已发送 email=xxx otp=xxxxxx`，不真正发送邮件 |

#### 调试时如何获取验证码 / OTP

1. 启动应用后，将日志级别调整为 DEBUG（或在 `application.properties` 加 `logging.level.com.ctrip=DEBUG`）。
2. 触发注册/发送验证码/忘记密码接口后，在日志中搜索 `[StubSms]` 或 `[StubEmail]` 关键字即可找到对应的验证码或 OTP。

#### 生产环境替换说明

生产上线前，需将桩实现替换为真实服务（如阿里云短信、SendGrid 等）：
1. 实现相同的 `SmsService` / `EmailService` 接口，注册为 `@Service @Profile("prod")` Bean；
2. 桩实现已标注 `@Profile("!prod")`，生产环境激活 `prod` profile 后自动失效，无需改动其他代码。

---

### 实施顺序

| 步骤 | 内容 | 状态 |
|------|------|------|
| 1 | 执行 DDL，建 3 张表 | ✅ 已完成 |
| 2 | `pom.xml` 新增依赖，验证编译通过 | ✅ 已完成 |
| 3 | `application.properties` 添加配置 | ✅ 已完成 |
| 4 | Enum：`UserStatus`、`Gender` | ✅ 已完成 |
| 5 | Entity：`User`、`RefreshToken`、`PasswordResetToken` | ✅ 已完成 |
| 6 | Mapper：`UserMapper`、`RefreshTokenMapper`、`PasswordResetTokenMapper` | ✅ 已完成 |
| 6.5 | `MybatisPlusConfig`（`MetaObjectHandler` 自动填充实现） | ✅ 已完成 |
| 7 | 公共层：`ApiResponse`、异常体系、`PhoneNumber`、`GlobalExceptionHandler` | ✅ 已完成 |
| 8 | `JwtConfig`（`@ConfigurationProperties`） | ✅ 已完成 |
| 9 | `JwtService` 接口 + `JwtServiceImpl` | ✅ 已完成 |
| 10 | `SecurityConfig` 骨架（先放行所有请求，让应用能启动） | ✅ 已完成 |
| 11 | `UserDetailsServiceImpl` | ✅ 已完成 |
| 12 | `JwtAuthenticationFilter` | ✅ 已完成 |
| 13 | `SecurityConfig` 最终版（锁定路由） | ✅ 已完成 |
| 14 | `AuthServiceImpl` | ✅ 已完成 |
| 15 | `UserServiceImpl` | ✅ 已完成 |
| 16 | `AuthController` | ✅ 已完成 |
| 17 | `UserController` | ✅ 已完成 |
| 18 | `RateLimitConfig` + `RateLimitInterceptor` + `WebConfig` | ✅ 已完成 |

---

### 代码审查记录

> 对照设计文档完成全量代码审查后，所有发现按优先级分级记录于此。
> 已修复项：C-2 ~ C-5、H-1 ~ H-6。
> 待修复项（MEDIUM / LOW）：记录于下表，后续迭代按优先级处理。

#### 已修复（C-2 ~ C-5 / H-1 ~ H-6）

| 编号 | 级别 | 问题 | 修复方式 |
|------|------|------|---------|
| C-1 | — | 开发期 DB 凭据硬编码明文 | 有意保留，见[上方说明](#数据库凭据c-1) |
| C-2 | HIGH | Stub 实现使用 `log.info` 打印 OTP，在聚合日志中可被采集 | 改为 `log.debug`；`@Profile("!prod")` 限定非生产环境激活 |
| C-3 | HIGH | `JwtAuthenticationFilter` / `SecurityConfig` / `RateLimitInterceptor` 手拼 JSON 字符串，格式不保证与 `ApiResponse` 一致 | 注入 `ObjectMapper`，统一调用 `objectMapper.writeValueAsString(ApiResponse.error(...))` |
| C-4 | HIGH | `RateLimitInterceptor.resolveClientIp` 信任可伪造的 `X-Forwarded-For` 头，攻击者可绕过 IP 限流 | 移除 XFF 读取，直接使用 `request.getRemoteAddr()`；生产通过 `server.forward-headers-strategy=NATIVE` 在容器层还原真实 IP |
| C-5 | HIGH | `CtripApplicationTests` 上下文加载依赖 MySQL，CI 环境无 DB 连接则测试失败 | 添加 H2 test scope 依赖；`@AutoConfigureTestDatabase(Replace.ANY)` + 注入测试用 JWT secret |
| H-1 | HIGH | `UserServiceImpl.getUserProfile` 缺少 `@Transactional(readOnly = true)` | 添加只读事务注解 |
| H-2 | MEDIUM | 注册端点返回 HTTP 200，语义上应为 201 Created | 改为 `ResponseEntity.status(HttpStatus.CREATED).body(...)` |
| H-3 | MEDIUM | `AuthServiceImpl` 直接依赖 `JwtConfig` 计算 refresh token 过期时间，破坏分层 | 将 `getAccessTokenExpiresInSeconds()` 和 `calculateRefreshTokenExpiry()` 移入 `JwtService` 接口，`AuthServiceImpl` 移除对 `JwtConfig` 的依赖 |
| H-4 | CRITICAL | `login` 中 SUSPENDED 检查在 BCrypt 验证之后，攻击者可通过不同响应判断密码是否正确 | SUSPENDED 检查提前至 BCrypt 之前：null 检查 → SUSPENDED 检查 → BCrypt 验证 |
| H-5 | HIGH | `updateProfile` 在 request 全为 null 字段时仍执行空 UPDATE | 添加 `hasUpdate` 守卫，无实际更新字段时跳过 `userMapper.update()` |
| H-6 | HIGH | `resetPassword` 用户不存在时抛 `"用户不存在"`，与 OTP 无效时信息不同，存在用户枚举风险 | 统一改为 `"OTP 无效或已过期"` |

#### 待修复 — MEDIUM

| 编号 | 问题 | 影响 | 建议方案 |
|------|------|------|---------|
| M-1 | `RateLimitInterceptor` 的 `ConcurrentHashMap<String, Bucket>` 无淘汰机制，长期运行 key 数量线性增长 | 内存泄漏，高流量下实例重启才能释放 | 替换为 Caffeine `Cache<String, Bucket>`（`expireAfterAccess(10, MINUTES)`，设置 `maximumSize`） |
| M-2 | `StubSmsServiceImpl` 验证码内存缓存未做并发安全的原子操作（put + expiry 检查分两步） | 极低概率的并发竞态，验证码可被重用 | 将存储结构改为 `ConcurrentHashMap<String, CodeEntry>`，用 `compute` 原子替换 |
| M-3 | `UserDetailsServiceImpl.loadUserByUsername` 按 email 查找，而 `AuthServiceImpl.login` 走独立的 `findUserByCredential`，两套查找路径不同步 | 未来若 `UserDetailsService` 路径被某场景激活，行为与预期不符 | 统一 `UserDetailsServiceImpl` 支持 email / phone 查找，或将 `findUserByCredential` 提取为共用私有方法 |

#### 第二轮审查发现并已修复（D-1 ~ D-3）

| 编号 | 问题 | 修复方式 |
|------|------|---------|
| D-1 | `AuthService.java` 第 68 行 Javadoc 写 `log.info`，与 C-2 修复后的实现不符 | 改为 `log.debug` |
| D-2 | `StubSmsServiceImpl.java` 类 Javadoc 写 `log.info`，同上 | 改为 `log.debug` |
| D-3 | `AuthServiceImpl.java` `login` 方法注释步骤编号重复（两处 "4."） | 第二个 "4." 改为 "5." |

#### 第二轮审查文档已补充（D-4 ~ D-6）

| 编号 | 问题 | 处理方式 |
|------|------|---------|
| D-4 | `RateLimitInterceptor` 实际在 `user/security/` 包，文档包结构未明确标注 | 文档包结构已添加该文件（属于 `user/security/`） |
| D-5 | `UpdateAvatarRequest` 存在于代码中但未在 DTO 定义章节列出 | 已补充到 DTO 定义章节 |
| D-6 | `RegisterByPhoneRequest.phone` 使用 `@PhoneNumber` 自定义注解，文档写的是 `@Pattern` | 已在 DTO 定义中更正并添加说明 |

#### 待修复 — LOW

| 编号 | 问题 | 影响 | 建议方案 |
|------|------|------|---------|
| L-1 | `password_reset_tokens` 使用后标记 `used=true` 但不清理，旧记录长期驻留 | DB 表持续增长 | 定时任务（每天）清理 `used=true` 或 `expires_at < NOW() - 7天` 的记录 |
| L-2 | `refresh_tokens` 中已吊销/过期的 token 不自动清理 | DB 表持续增长，历史 token 查询干扰 | 同上，定时任务清理 `revoked=true` 或 `expires_at < NOW()` 的记录 |
| L-3 | `ChangePasswordRequest` 无旧密码强度 / 新旧密码相同校验 | 用户可将密码改为与旧密码相同的值而无感知 | 在 `UserServiceImpl.changePassword` 中加相等性检查 |
| L-4 | `UserConverter` 将 `UserStatus` 枚举直接 `.name()` 暴露给前端，枚举重命名会破坏 API 协议 | 接口不稳定 | 定义固定的字符串常量映射，与枚举内部名解耦 |
