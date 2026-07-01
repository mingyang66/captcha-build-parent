package com.emily.captcha.otp;

/**
 * OTP哈希算法枚举
 * <p>
 * 定义支持的HMAC算法，用于TOTP密码生成和验证
 * <p>
 * 基于 RFC 6238 标准，推荐使用 HmacSHA1（兼容性最好）
 *
 * @author Emily
 * @since 1.0.0
 */
public enum OtpHashAlgorithm {

    /**
     * HMAC-SHA1 算法
     * <p>
     * 默认算法，兼容性最好，所有OTP应用都支持
     */
    HMAC_SHA1("HmacSHA1", "SHA1", 20),

    /**
     * HMAC-SHA256 算法
     * <p>
     * 更强的安全性，部分OTP应用支持
     */
    HMAC_SHA256("HmacSHA256", "SHA256", 32),

    /**
     * HMAC-SHA512 算法
     * <p>
     * 最高安全性，少数OTP应用支持
     */
    HMAC_SHA512("HmacSHA512", "SHA512", 64);

    /**
     * Java加密算法名称（用于Mac.getInstance()）
     */
    private final String javaAlgorithm;

    /**
     * OTP Auth URI中的算法名称
     */
    private final String uriAlgorithm;

    /**
     * RFC 6238 推荐的密钥长度（字节）
     */
    private final int recommendedKeyLength;

    OtpHashAlgorithm(String javaAlgorithm, String uriAlgorithm, int recommendedKeyLength) {
        this.javaAlgorithm = javaAlgorithm;
        this.uriAlgorithm = uriAlgorithm;
        this.recommendedKeyLength = recommendedKeyLength;
    }

    /**
     * 获取Java加密算法名称
     *
     * @return Java算法名称，如 "HmacSHA1"
     */
    public String getJavaAlgorithm() {
        return javaAlgorithm;
    }

    /**
     * 获取OTP Auth URI中的算法名称
     *
     * @return URI算法名称，如 "SHA1"
     */
    public String getUriAlgorithm() {
        return uriAlgorithm;
    }

    /**
     * 获取RFC 6238推荐的密钥长度
     *
     * @return 密钥长度（字节）
     */
    public int getRecommendedKeyLength() {
        return recommendedKeyLength;
    }

    /**
     * 根据Java算法名称获取枚举
     *
     * @param javaAlgorithm Java算法名称
     * @return 对应的枚举值
     * @throws IllegalArgumentException 如果算法名称不支持
     */
    public static OtpHashAlgorithm fromJavaAlgorithm(String javaAlgorithm) {
        for (OtpHashAlgorithm algorithm : values()) {
            if (algorithm.javaAlgorithm.equalsIgnoreCase(javaAlgorithm)) {
                return algorithm;
            }
        }
        throw new IllegalArgumentException("不支持的OTP算法: " + javaAlgorithm +
                "，支持的算法: HMAC_SHA1, HMAC_SHA256, HMAC_SHA512");
    }

    /**
     * 根据URI算法名称获取枚举
     *
     * @param uriAlgorithm URI算法名称
     * @return 对应的枚举值
     * @throws IllegalArgumentException 如果算法名称不支持
     */
    public static OtpHashAlgorithm fromUriAlgorithm(String uriAlgorithm) {
        for (OtpHashAlgorithm algorithm : values()) {
            if (algorithm.uriAlgorithm.equalsIgnoreCase(uriAlgorithm)) {
                return algorithm;
            }
        }
        throw new IllegalArgumentException("不支持的OTP算法: " + uriAlgorithm +
                "，支持的算法: SHA1, SHA256, SHA512");
    }
}
