package com.emily.captcha;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 验证码配置属性
 */
@ConfigurationProperties(prefix = CaptchaProperties.PREFIX)
public class CaptchaProperties {
    /**
     * 配置属性前缀
     */
    public static final String PREFIX = "spring.emily.captcha";
    /**
     * 是否启用
     */
    private boolean enabled = true;
    /**
     * 点击验证码配置
     */
    private Click click = new Click();
    /**
     * 滑动验证码配置
     */
    private Slider slider = new Slider();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Click getClick() {
        return click;
    }

    public void setClick(Click click) {
        this.click = click;
    }

    public Slider getSlider() {
        return slider;
    }

    public void setSlider(Slider slider) {
        this.slider = slider;
    }

    public static class Click {
        /**
         * 图片宽度
         */
        private int width = 320;

        /**
         * 图片高度
         */
        private int height = 160;

        /**
         * 图片中显示的字符总数
         */
        private int charCount = 5;

        /**
         * 需要用户按顺序点击的目标字符数
         */
        private int targetCount = 3;

        /**
         * 字体最小尺寸
         */
        private int fontSizeMin = 22;

        /**
         * 字体最大尺寸
         */
        private int fontSizeMax = 30;

        /**
         * 干扰线数量
         */
        private int noiseLineCount = 6;

        /**
         * 干扰点数量
         */
        private int noisePointCount = 30;

        /**
         * 验证码过期时间（秒）
         */
        private Duration expiryTime = Duration.ofSeconds(120);

        /**
         * 点击容差（像素），判定点击是否命中字符中心
         */
        private int tolerance = 20;

        public int getWidth() {
            return width;
        }

        public void setWidth(int width) {
            this.width = width;
        }

        public int getHeight() {
            return height;
        }

        public void setHeight(int height) {
            this.height = height;
        }

        public int getCharCount() {
            return charCount;
        }

        public void setCharCount(int charCount) {
            this.charCount = charCount;
        }

        public int getTargetCount() {
            return targetCount;
        }

        public void setTargetCount(int targetCount) {
            this.targetCount = targetCount;
        }

        public int getFontSizeMin() {
            return fontSizeMin;
        }

        public void setFontSizeMin(int fontSizeMin) {
            this.fontSizeMin = fontSizeMin;
        }

        public int getFontSizeMax() {
            return fontSizeMax;
        }

        public void setFontSizeMax(int fontSizeMax) {
            this.fontSizeMax = fontSizeMax;
        }

        public int getNoiseLineCount() {
            return noiseLineCount;
        }

        public void setNoiseLineCount(int noiseLineCount) {
            this.noiseLineCount = noiseLineCount;
        }

        public int getNoisePointCount() {
            return noisePointCount;
        }

        public void setNoisePointCount(int noisePointCount) {
            this.noisePointCount = noisePointCount;
        }

        public Duration getExpiryTime() {
            return expiryTime;
        }

        public void setExpiryTime(Duration expiryTime) {
            this.expiryTime = expiryTime;
        }

        public int getTolerance() {
            return tolerance;
        }

        public void setTolerance(int tolerance) {
            this.tolerance = tolerance;
        }
    }

    // ---------- 滑动验证码配置 ----------

    public static class Slider {
        /**
         * 滑动验证码背景图宽度
         */
        private int width = 320;

        /**
         * 滑动验证码背景图高度
         */
        private int height = 160;

        /**
         * 滑动验证码X坐标容差（像素）
         */
        private int tolerance = 5;

        /**
         * 干扰线数量
         */
        private int noiseLineCount = 6;

        /**
         * 干扰点数量
         */
        private int noisePointCount = 30;

        /**
         * 验证码过期时间
         */
        private Duration expiryTime = Duration.ofSeconds(120);

        public int getWidth() {
            return width;
        }

        public void setWidth(int width) {
            this.width = width;
        }

        public int getHeight() {
            return height;
        }

        public void setHeight(int height) {
            this.height = height;
        }

        public int getTolerance() {
            return tolerance;
        }

        public void setTolerance(int tolerance) {
            this.tolerance = tolerance;
        }

        public int getNoiseLineCount() {
            return noiseLineCount;
        }

        public void setNoiseLineCount(int noiseLineCount) {
            this.noiseLineCount = noiseLineCount;
        }

        public int getNoisePointCount() {
            return noisePointCount;
        }

        public void setNoisePointCount(int noisePointCount) {
            this.noisePointCount = noisePointCount;
        }

        public Duration getExpiryTime() {
            return expiryTime;
        }

        public void setExpiryTime(Duration expiryTime) {
            this.expiryTime = expiryTime;
        }
    }
}
