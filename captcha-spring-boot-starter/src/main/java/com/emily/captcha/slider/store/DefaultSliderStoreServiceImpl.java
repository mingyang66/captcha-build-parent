package com.emily.captcha.slider.store;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 滑动验证码会话默认内存存储实现
 */
public class DefaultSliderStoreServiceImpl implements SliderStoreService {

    private final ConcurrentHashMap<String, SliderCaptchaSession> store = new ConcurrentHashMap<>();

    @Override
    public void put(String key, SliderCaptchaSession value) {
        store.put(key, value);
    }

    @Override
    public SliderCaptchaSession remove(String key) {
        return store.remove(key);
    }

    @Override
    public void cleanExpired() {
        long now = System.currentTimeMillis();
        store.entrySet().removeIf(entry -> entry.getValue().expireAt() < now);
    }
}
