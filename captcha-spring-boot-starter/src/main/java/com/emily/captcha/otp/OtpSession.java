package com.emily.captcha.otp;

/**
 * OTP会话数据
 * <p>
 * 存储用户的OTP密钥和验证状态
 */
public class OtpSession {
    /**
     * 用户的OTP密钥（Base32编码）
     */
    private String secret;

    /**
     * 账户标识（用户名或邮箱）
     */
    private String account;

    /**
     * 是否已验证
     */
    private boolean verified;

    /**
     * 最后验证时间戳（毫秒）
     */
    private long lastVerifiedAt;

    /**
     * 上次使用的OTP密码（用于防重放攻击）
     */
    private String lastUsedOtp;

    /**
     * 上次使用时间戳（毫秒）
     */
    private long lastUsedAt;

    public OtpSession() {
    }

    public OtpSession(String secret, String account) {
        this.secret = secret;
        this.account = account;
        this.verified = false;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public long getLastVerifiedAt() {
        return lastVerifiedAt;
    }

    public void setLastVerifiedAt(long lastVerifiedAt) {
        this.lastVerifiedAt = lastVerifiedAt;
    }

    public String getLastUsedOtp() {
        return lastUsedOtp;
    }

    public void setLastUsedOtp(String lastUsedOtp) {
        this.lastUsedOtp = lastUsedOtp;
    }

    public long getLastUsedAt() {
        return lastUsedAt;
    }

    public void setLastUsedAt(long lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }
}
