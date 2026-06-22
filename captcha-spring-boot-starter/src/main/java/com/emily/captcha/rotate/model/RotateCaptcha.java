package com.emily.captcha.rotate.model;

/**
 * 旋转验证码生成结果
 */
public class RotateCaptcha {

    /**
     * 验证码唯一标识
     */
    private String captchaId;

    /**
     * 被旋转后的圆形图片 Base64（PNG格式，含 data:image/png;base64, 前缀）
     */
    private String image;

    public String getCaptchaId() {
        return captchaId;
    }

    public void setCaptchaId(String captchaId) {
        this.captchaId = captchaId;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }
}
