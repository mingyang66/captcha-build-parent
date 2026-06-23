package com.emily.captcha.otp;

import java.time.Duration;

/**
 * OTP（一次性密码）配置属性
 * <p>
 * 基于 RFC 6238 (TOTP) 标准实现
 */
public class OtpProperties {
    /**
     * OTP密码长度（默认6位）
     */
    private int codeLength = 6;

    /**
     * 时间步长（默认30秒），生成器刷新周期
     */
    private Duration timeStep = Duration.ofSeconds(30);

    /**
     * 允许的时间窗口偏移（默认1），用于处理时钟不同步
     * <p>
     * 例如：设置为1表示允许当前时间步长的前一个和后一个时间步长的密码都有效
     */
    private int windowSize = 1;

    /**
     * 密钥长度（字节），默认20字节（160位）
     */
    private int secretKeyLength = 20;

    /**
     * 哈希算法（默认HmacSHA1）
     * <p>
     * 可选值：HmacSHA1, HmacSHA256, HmacSHA512
     */
    private String algorithm = "HmacSHA1";

    public int getCodeLength() {
        return codeLength;
    }

    public void setCodeLength(int codeLength) {
        this.codeLength = codeLength;
    }

    public Duration getTimeStep() {
        return timeStep;
    }

    public void setTimeStep(Duration timeStep) {
        this.timeStep = timeStep;
    }

    public int getWindowSize() {
        return windowSize;
    }

    public void setWindowSize(int windowSize) {
        this.windowSize = windowSize;
    }

    public int getSecretKeyLength() {
        return secretKeyLength;
    }

    public void setSecretKeyLength(int secretKeyLength) {
        this.secretKeyLength = secretKeyLength;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }
}
