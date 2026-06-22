package com.emily.captcha.slider.store;

/**
 * 滑动验证码会话存储接口
 */
public interface SliderStoreService {

    /**
     * 存储滑动验证码会话
     */
    void put(String key, SliderCaptchaSession value);

    /**
     * 移除并返回指定key的会话
     */
    SliderCaptchaSession remove(String key);

    /**
     * 清理已过期的会话
     */
    void cleanExpired();
}
