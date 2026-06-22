package com.emily.captcha.click.store;

public interface ClickStoreService {
    /**
     * 存储key-value
     */
    void put(String key, CaptchaSession value);

    /**
     * 移除已经过期的key
     */
    void cleanExpired();

    /**
     * 移除指定key
     */
    CaptchaSession remove(String key);
}
