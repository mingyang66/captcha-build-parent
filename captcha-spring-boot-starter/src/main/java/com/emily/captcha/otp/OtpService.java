package com.emily.captcha.otp;

import com.emily.captcha.CaptchaProperties;

import java.util.Objects;

/**
 * OTP（一次性密码）核心服务
 * <p>
 * 提供OTP密钥管理、生成和验证功能
 * <p>
 * 基于 RFC 6238 (TOTP) 标准实现，兼容Google Authenticator、Microsoft Authenticator等应用
 */
public class OtpService {

    /**
     * 验证码配置属性
     */
    private final CaptchaProperties properties;

    /**
     * OTP会话存储服务
     */
    private final OtpStoreService otpStoreService;

    public OtpService(CaptchaProperties properties, OtpStoreService otpStoreService) {
        this.properties = properties;
        this.otpStoreService = otpStoreService;
    }

    /**
     * 为用户生成新的OTP密钥
     * <p>
     * 此方法应该只在用户首次启用OTP时调用
     *
     * @param account 账户标识（用户名或邮箱）
     * @return Base32编码的OTP密钥
     */
    public String generateSecret(String account) {
        Objects.requireNonNull(account, "账户标识不能为空");

        // 生成Base32编码的密钥
        String secret = OtpSecretGenerator.generateBase32Secret(properties.getOtp().getSecretKeyLength());

        // 创建并存储会话
        OtpSession session = new OtpSession(secret, account);
        otpStoreService.put(account, session);

        return secret;
    }

    /**
     * 获取用户的OTP密钥
     *
     * @param account 账户标识
     * @return Base32编码的OTP密钥，如果不存在则返回null
     */
    public String getSecret(String account) {
        OtpSession session = otpStoreService.get(account);
        return session != null ? session.getSecret() : null;
    }

    /**
     * 验证OTP密码
     * <p>
     * 验证用户输入的OTP密码是否正确，包含防重放攻击保护
     *
     * @param account 账户标识
     * @param otp     用户输入的OTP密码
     * @return true=验证通过，false=验证失败
     */
    public boolean verify(String account, String otp) {
        Objects.requireNonNull(account, "账户标识不能为空");
        Objects.requireNonNull(otp, "OTP密码不能为空");

        // 获取用户的OTP会话
        OtpSession session = otpStoreService.get(account);
        if (session == null) {
            return false;
        }

        String secret = session.getSecret();
        if (secret == null || secret.isEmpty()) {
            return false;
        }

        // 获取配置参数
        long timeStep = properties.getOtp().getTimeStep().getSeconds();
        int windowSize = properties.getOtp().getWindowSize();
        int codeLength = properties.getOtp().getCodeLength();
        String algorithm = properties.getOtp().getAlgorithm();

        // 验证OTP密码
        boolean isValid = OtpAlgorithm.verifyTotp(
                secret, otp, System.currentTimeMillis(),
                timeStep, windowSize, codeLength, algorithm);

        if (isValid) {
            // 防重放攻击检查：同一时间窗口内的OTP只能使用一次
            long currentTimeWindow = System.currentTimeMillis() / (timeStep * 1000);
            long lastUsedWindow = session.getLastUsedAt() / (timeStep * 1000);

            if (currentTimeWindow == lastUsedWindow && Objects.equals(otp, session.getLastUsedOtp())) {
                return false; // OTP已被使用过
            }

            // 更新会话状态
            session.setLastUsedOtp(otp);
            session.setLastUsedAt(System.currentTimeMillis());
            session.setVerified(true);
            session.setLastVerifiedAt(System.currentTimeMillis());
            otpStoreService.update(account, session);
        }

        return isValid;
    }

    /**
     * 生成OTP Auth URI（用于生成QR码）
     * <p>
     * 用户可以扫描此QR码在Google Authenticator等应用中添加账户
     *
     * @param account 账户标识
     * @param issuer  发行方标识（公司或应用名称）
     * @return OTP Auth URI，如果账户不存在则返回null
     */
    public String generateOtpAuthUri(String account, String issuer) {
        String secret = getSecret(account);
        if (secret == null) {
            return null;
        }

        return OtpSecretGenerator.generateOtpAuthUri(secret, account, issuer);
    }

    /**
     * 删除用户的OTP配置
     *
     * @param account 账户标识
     */
    public void remove(String account) {
        otpStoreService.remove(account);
    }

    /**
     * 检查用户是否已启用OTP
     *
     * @param account 账户标识
     * @return true=已启用，false=未启用
     */
    public boolean isEnabled(String account) {
        return getSecret(account) != null;
    }
}
