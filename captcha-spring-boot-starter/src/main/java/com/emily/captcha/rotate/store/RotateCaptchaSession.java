package com.emily.captcha.rotate.store;

/**
 * 旋转验证码会话数据
 *
 * @param targetAngle 目标旋转角度（度），用户需要将图片旋转到此角度的反向以还原
 * @param expireAt    过期时间戳（毫秒）
 */
public record RotateCaptchaSession(int targetAngle, long expireAt) {

}
