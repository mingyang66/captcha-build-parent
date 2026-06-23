package com.emily.captcha.otp;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

/**
 * TOTP（基于时间的一次性密码）算法实现
 * <p>
 * 基于 RFC 6238 标准实现
 * <p>
 * TOTP = HOTP(K, T)
 * 其中：
 * - K 是共享密钥
 * - T 是时间计数（当前时间戳 / 时间步长）
 */
public class OtpAlgorithm {

    /**
     * 根据时间戳生成TOTP密码
     *
     * @param secret     共享密钥（Base32编码）
     * @param timestamp  当前时间戳（毫秒）
     * @param timeStep   时间步长（秒）
     * @param codeLength 密码长度
     * @param algorithm  哈希算法（HmacSHA1/HmacSHA256/HmacSHA512）
     * @return TOTP密码字符串
     */
    public static String generateTotp(String secret, long timestamp, long timeStep, int codeLength, String algorithm) {
        // 1. 解码Base32密钥
        byte[] key = OtpSecretGenerator.decodeBase32(secret);

        // 2. 计算时间计数
        long timeCounter = timestamp / (timeStep * 1000);

        // 3. 生成HOTP
        return generateHotp(key, timeCounter, codeLength, algorithm);
    }

    /**
     * 根据时间戳生成TOTP密码（字节数组密钥）
     *
     * @param secret     共享密钥（字节数组）
     * @param timestamp  当前时间戳（毫秒）
     * @param timeStep   时间步长（秒）
     * @param codeLength 密码长度
     * @param algorithm  哈希算法
     * @return TOTP密码字符串
     */
    public static String generateTotp(byte[] secret, long timestamp, long timeStep, int codeLength, String algorithm) {
        // 1. 计算时间计数
        long timeCounter = timestamp / (timeStep * 1000);

        // 2. 生成HOTP
        return generateHotp(secret, timeCounter, codeLength, algorithm);
    }

    /**
     * 生成HOTP（基于HMAC的一次性密码）
     * <p>
     * 基于 RFC 4226 标准
     *
     * @param key        共享密钥
     * @param counter    计数器
     * @param codeLength 密码长度
     * @param algorithm  哈希算法
     * @return HOTP密码字符串
     */
    private static String generateHotp(byte[] key, long counter, int codeLength, String algorithm) {
        try {
            // 1. 将计数器转换为8字节数组（大端序）
            byte[] counterBytes = ByteBuffer.allocate(8).putLong(counter).array();

            // 2. 计算HMAC
            Mac mac = Mac.getInstance(algorithm);
            SecretKeySpec keySpec = new SecretKeySpec(key, algorithm);
            mac.init(keySpec);
            byte[] hmacResult = mac.doFinal(counterBytes);

            // 3. 动态截断（Dynamic Truncation）
            int offset = hmacResult[hmacResult.length - 1] & 0x0F;
            int binary = ((hmacResult[offset] & 0x7F) << 24)
                    | ((hmacResult[offset + 1] & 0xFF) << 16)
                    | ((hmacResult[offset + 2] & 0xFF) << 8)
                    | (hmacResult[offset + 3] & 0xFF);

            // 4. 生成指定长度的密码
            int otp = binary % (int) Math.pow(10, codeLength);

            // 5. 左侧补零
            return String.format("%0" + codeLength + "d", otp);

        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("OTP生成失败: " + e.getMessage(), e);
        }
    }

    /**
     * 验证TOTP密码
     *
     * @param secret     共享密钥（Base32编码）
     * @param otp        用户输入的OTP密码
     * @param timestamp  当前时间戳（毫秒）
     * @param timeStep   时间步长（秒）
     * @param windowSize 时间窗口大小
     * @param codeLength 密码长度
     * @param algorithm  哈希算法
     * @return true=验证通过，false=验证失败
     */
    public static boolean verifyTotp(String secret, String otp, long timestamp, long timeStep,
                                     int windowSize, int codeLength, String algorithm) {
        Objects.requireNonNull(secret, "密钥不能为空");
        Objects.requireNonNull(otp, "OTP密码不能为空");

        // 在当前时间窗口及其前后窗口内验证
        long currentTimeCounter = timestamp / (timeStep * 1000);

        for (int i = -windowSize; i <= windowSize; i++) {
            long adjustedTime = (currentTimeCounter + i) * timeStep * 1000;
            String generatedOtp = generateTotp(secret, adjustedTime, timeStep, codeLength, algorithm);

            if (Objects.equals(otp, generatedOtp)) {
                return true;
            }
        }

        return false;
    }
}
