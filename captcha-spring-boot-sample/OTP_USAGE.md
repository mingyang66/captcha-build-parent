# OTP 双因子认证使用指南

## 📱 功能概述

OTP（One-Time Password）双因子认证基于 **RFC 6238 (TOTP)** 标准实现，兼容 Google Authenticator、Microsoft Authenticator 等主流验证器应用。

## 🚀 快速开始

### 1. 启动示例应用

```bash
cd captcha-spring-boot-sample
mvn spring-boot:run
```

### 2. 访问OTP页面

打开浏览器访问：`http://localhost:8080/otp.html`

## 📋 使用步骤

### 步骤1：启用OTP

1. 在"账户标识"输入框中输入您的账户（例如：`user@example.com`）
2. 点击"生成密钥"按钮
3. 系统会显示：
   - **OTP密钥**：Base32编码的密钥字符串
   - **QR码**：可直接扫描的二维码
   - **OTP Auth URI**：用于生成QR码的URI

### 步骤2：配置验证器应用

#### 方式一：扫描二维码（推荐）

1. 打开 Google Authenticator 或其他OTP应用
2. 点击"+"添加账户
3. 选择"扫描二维码"
4. 扫描页面上的QR码

#### 方式二：手动输入密钥

1. 打开验证器应用
2. 点击"+"添加账户
3. 选择"手动输入密钥"
4. 输入以下信息：
   - **账户**：您的账户标识
   - **密钥**：页面上显示的密钥字符串
   - **类型**：基于时间

### 步骤3：验证OTP

1. 在验证器应用中查看当前6位OTP密码
2. 在页面的"6位OTP密码"输入框中输入该密码
3. 点击"验证OTP"按钮
4. 系统会显示验证结果

## 🔧 REST API 接口

### 生成OTP密钥

```http
POST /api/captcha/otp/secret?account=user@example.com
```

**响应：**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "account": "user@example.com",
    "secret": "JBSWY3DPEHPK3PXP",
    "otpAuthUri": "otpauth://totp/EmilyCaptcha:user@example.com?secret=JBSWY3DPEHPK3PXP&issuer=EmilyCaptcha&algorithm=SHA1&digits=6&period=30"
  }
}
```

### 检查OTP状态

```http
GET /api/captcha/otp/enabled?account=user@example.com
```

**响应：**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "account": "user@example.com",
    "enabled": true
  }
}
```

### 验证OTP密码

```http
POST /api/captcha/otp/verify
Content-Type: application/json

{
  "account": "user@example.com",
  "otp": "123456"
}
```

**响应：**
```json
{
  "code": 200,
  "message": "验证通过"
}
```

### 删除OTP配置

```http
POST /api/captcha/otp/remove?account=user@example.com
```

**响应：**
```json
{
  "code": 200,
  "message": "OTP已删除"
}
```

## ⚙️ 配置选项

在 `application.properties` 中可以配置OTP参数：

```properties
# OTP密码长度（默认6位）
spring.emily.captcha.otp.code-length=6

# 时间步长（默认30秒）
spring.emily.captcha.otp.time-step=PT30S

# 时间窗口大小（默认1，允许前后1个时间窗口）
spring.emily.captcha.otp.window-size=1

# 密钥长度（默认20字节）
spring.emily.captcha.otp.secret-key-length=20

# 哈希算法（默认HmacSHA1）
spring.emily.captcha.otp.algorithm=HmacSHA1
```

## 🔒 安全特性

- ✅ **时间窗口验证**：允许前后时间窗口的密码，处理时钟不同步
- ✅ **防重放攻击**：同一时间窗口内的OTP只能使用一次
- ✅ **Base32编码**：兼容所有主流验证器应用
- ✅ **可配置算法**：支持HmacSHA1、HmacSHA256、HmacSHA512
- ✅ **可扩展存储**：支持自定义存储实现（Redis等）

## 💡 使用建议

1. **密钥安全**：生成后请安全保存密钥，不要泄露
2. **时钟同步**：确保服务器时间准确（建议使用NTP同步）
3. **备份密钥**：建议用户备份密钥，以防手机丢失
4. **测试验证**：启用后立即测试验证，确保配置正确

## 🎯 集成到您的应用

```java
@Autowired
private OtpService otpService;

// 1. 为用户生成OTP密钥
public String enableOtp(String account) {
    String secret = otpService.generateSecret(account);
    String uri = otpService.generateOtpAuthUri(account, "YourApp");
    // 将URI生成QR码返回给用户
    return uri;
}

// 2. 验证用户登录时的OTP
public boolean login(String account, String password, String otp) {
    // 先验证用户名密码
    if (!validatePassword(account, password)) {
        return false;
    }
    
    // 再验证OTP
    if (otpService.isEnabled(account)) {
        return otpService.verify(account, otp);
    }
    
    return true;
}
```

## 📚 相关文档

- [RFC 6238 - TOTP标准](https://tools.ietf.org/html/rfc6238)
- [RFC 4226 - HOTP标准](https://tools.ietf.org/html/rfc4226)
- [Google Authenticator](https://support.google.com/accounts/answer/1066447)
- [Microsoft Authenticator](https://support.microsoft.com/account-billing/download-and-install-the-microsoft-authenticator-app-351498fc-850a-45da-b7b6-27e523b8702a)
