package com.emily.captcha.slider.store;

/**
 * 滑动验证码会话数据
 *
 * @param targetX  拼图块目标X坐标（像素）
 * @param expireAt 过期时间戳（毫秒）
 */
public record SliderCaptchaSession(int targetX, long expireAt) {

}
