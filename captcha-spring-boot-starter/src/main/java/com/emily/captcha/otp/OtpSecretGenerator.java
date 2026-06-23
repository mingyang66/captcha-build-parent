package com.emily.captcha.otp;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * OTP密钥生成器
 * <p>
 * 用于生成和编码OTP密钥，支持Base32编码（兼容Google Authenticator等应用）
 */
public class OtpSecretGenerator {

    private static final String BASE32_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * 生成随机密钥（字节数组）
     *
     * @param length 密钥长度（字节）
     * @return 随机密钥字节数组
     */
    public static byte[] generateSecret(int length) {
        byte[] secret = new byte[length];
        SECURE_RANDOM.nextBytes(secret);
        return secret;
    }

    /**
     * 生成Base32编码的密钥字符串
     * <p>
     * Base32编码兼容Google Authenticator、Microsoft Authenticator等OTP应用
     *
     * @param length 密钥长度（字节）
     * @return Base32编码的密钥字符串
     */
    public static String generateBase32Secret(int length) {
        byte[] secret = generateSecret(length);
        return encodeBase32(secret);
    }

    /**
     * 将字节数组编码为Base32字符串
     *
     * @param bytes 字节数组
     * @return Base32编码字符串
     */
    public static String encodeBase32(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        int buffer = 0;
        int bufferLength = 0;

        for (byte b : bytes) {
            buffer = (buffer << 8) | (b & 0xFF);
            bufferLength += 8;

            while (bufferLength >= 5) {
                bufferLength -= 5;
                sb.append(BASE32_CHARS.charAt((buffer >> bufferLength) & 0x1F));
            }
        }

        if (bufferLength > 0) {
            sb.append(BASE32_CHARS.charAt((buffer << (5 - bufferLength)) & 0x1F));
        }

        return sb.toString();
    }

    /**
     * 将Base32字符串解码为字节数组
     *
     * @param base32 Base32编码字符串
     * @return 字节数组
     */
    public static byte[] decodeBase32(String base32) {
        if (base32 == null || base32.isEmpty()) {
            throw new IllegalArgumentException("Base32字符串不能为空");
        }

        // 移除空格和连字符
        base32 = base32.toUpperCase().replaceAll("[\\s-]", "");

        int buffer = 0;
        int bufferLength = 0;
        int byteCount = (base32.length() * 5 + 7) / 8;
        byte[] bytes = new byte[byteCount];
        int byteIndex = 0;

        for (char c : base32.toCharArray()) {
            int value = BASE32_CHARS.indexOf(c);
            if (value < 0) {
                throw new IllegalArgumentException("无效的Base32字符: " + c);
            }

            buffer = (buffer << 5) | value;
            bufferLength += 5;

            if (bufferLength >= 8) {
                bufferLength -= 8;
                bytes[byteIndex++] = (byte) ((buffer >> bufferLength) & 0xFF);
            }
        }

        return bytes;
    }

    /**
     * 生成QR码的URI（用于Google Authenticator等应用扫描）
     *
     * @param secret  Base32编码的密钥
     * @param account 账户标识（通常是邮箱或用户名）
     * @param issuer  发行方标识（通常是公司或应用名称）
     * @return OTP Auth URI
     */
    public static String generateOtpAuthUri(String secret, String account, String issuer) {
        return String.format("otpauth://totp/%s:%s?secret=%s&issuer=%s&algorithm=%s&digits=6&period=30",
                issuer, account, secret, issuer, "SHA1");
    }
}
