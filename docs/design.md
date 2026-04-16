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
  - [实施顺序](#实施顺序)

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
│   ├── RateLimitConfig.java
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
    │   └── EmailService.java               # 接口（对外发邮件）
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
    │   └── UserDetailsServiceImpl.java
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
```

---

### application.properties 配置

```properties
spring.application.name=ctrip

# DataSource
spring.datasource.url=jdbc:mysql://localhost:3306/ctrip?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=UTF-8
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# MyBatis Plus
mybatis-plus.mapper-locations=classpath*:mapper/**/*.xml
mybatis-plus.type-aliases-package=com.ctrip.user.entity
mybatis-plus.configuration.map-underscore-to-camel-case=true
mybatis-plus.configuration.log-impl=org.apache.ibatis.logging.nologging.NoLoggingImpl
mybatis-plus.global-config.db-config.id-type=auto
# 枚举包扫描：让 MyBatis Plus 识别 @EnumValue 注解，自动完成枚举与数据库值的互转
mybatis-plus.type-enums-package=com.ctrip.user.entity.enums
# 注意：不使用 MyBatis Plus 逻辑删除插件，软删除通过 UserStatus.DELETED（status=3）实现

# JWT
app.jwt.secret=${JWT_SECRET}
app.jwt.access-token-expiration-ms=900000
app.jwt.refresh-token-expiration-ms=1296000000
app.jwt.issuer=ctrip

# 限流（认证端点，每 IP 每分钟）
app.rate-limit.auth.capacity=10
app.rate-limit.auth.refill-tokens=10
app.rate-limit.auth.refill-duration-seconds=60
```

---

### API 端点

#### 认证端点（无需 JWT）— `AuthController`

| Method | Path | 说明 |
|--------|------|------|
| POST | `/api/v1/auth/register/phone` | 手机号 + 短信验证码注册 |
| POST | `/api/v1/auth/register/email` | 邮箱 + 密码注册 |
| POST | `/api/v1/auth/login` | 登录（手机/邮箱 + 密码） |
| POST | `/api/v1/auth/refresh` | 刷新 access token |
| POST | `/api/v1/auth/logout` | 登出（吊销 refresh token） |
| POST | `/api/v1/auth/password/forgot` | 发起密码重置 |
| POST | `/api/v1/auth/password/reset` | 完成密码重置 |
| POST | `/api/v1/auth/sms/send` | 发送短信验证码 |

#### 用户端点（需 JWT）— `UserController`

| Method | Path | 说明 |
|--------|------|------|
| GET | `/api/v1/users/me` | 查看个人资料 |
| PUT | `/api/v1/users/me` | 更新个人资料 |
| PUT | `/api/v1/users/me/password` | 修改密码 |
| PUT | `/api/v1/users/me/avatar` | 更新头像 URL |

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

public record RegisterByPhoneRequest(
    @NotBlank @Pattern(regexp = "^\\+?[1-9]\\d{7,14}$") String phone,
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

### 实施顺序

| 步骤 | 内容 | 状态 |
|------|------|------|
| 1 | 执行 DDL，建 3 张表 | ⏳ 待执行（需本地 MySQL 手动操作） |
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
| 11 | `UserDetailsServiceImpl` | 待完成 |
| 12 | `JwtAuthenticationFilter` | 待完成 |
| 13 | `SecurityConfig` 最终版（锁定路由） | 待完成 |
| 14 | `AuthServiceImpl` | 待完成 |
| 15 | `UserServiceImpl` | 待完成 |
| 16 | `AuthController` | 待完成 |
| 17 | `UserController` | 待完成 |
| 18 | `RateLimitConfig` + `RateLimitInterceptor` | 待完成 |
