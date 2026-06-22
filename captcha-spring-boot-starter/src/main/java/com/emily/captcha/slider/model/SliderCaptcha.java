package com.emily.captcha.slider.model;

/**
 * 滑动验证码生成结果
 */
public class SliderCaptcha {

    /**
     * 验证码唯一标识
     */
    private String captchaId;

    /**
     * 带缺口的背景图片 Base64（PNG格式，含 data:image/png;base64, 前缀）
     */
    private String backgroundImage;

    /**
     * 滑块拼图块图片 Base64（PNG格式，含 data:image/png;base64, 前缀）
     */
    private String sliderImage;

    /**
     * 滑块拼图块的Y坐标（前端用于定位滑块垂直位置）
     */
    private int y;

    public String getCaptchaId() {
        return captchaId;
    }

    public void setCaptchaId(String captchaId) {
        this.captchaId = captchaId;
    }

    public String getBackgroundImage() {
        return backgroundImage;
    }

    public void setBackgroundImage(String backgroundImage) {
        this.backgroundImage = backgroundImage;
    }

    public String getSliderImage() {
        return sliderImage;
    }

    public void setSliderImage(String sliderImage) {
        this.sliderImage = sliderImage;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }
}
