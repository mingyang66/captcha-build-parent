package com.emily.captcha.rotate.store;

/**
 * 旋转验证码会话存储接口
 */
public interface RotateStoreService {

    /**
     * 存储旋转验证码会话
     */
    void put(String key, RotateCaptchaSession value);

    /**
     * 移除并返回指定key的会话
     */
    RotateCaptchaSession remove(String key);

    /**
     * 清理已过期的会话
     */
    void cleanExpired();
}
