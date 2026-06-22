package com.emily.captcha.rotate.store;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 旋转验证码会话默认内存存储实现
 */
public class DefaultRotateStoreServiceImpl implements RotateStoreService {

    private final ConcurrentHashMap<String, RotateCaptchaSession> store = new ConcurrentHashMap<>();

    @Override
    public void put(String key, RotateCaptchaSession value) {
        store.put(key, value);
    }

    @Override
    public RotateCaptchaSession remove(String key) {
        return store.remove(key);
    }

    @Override
    public void cleanExpired() {
        long now = System.currentTimeMillis();
        store.entrySet().removeIf(entry -> entry.getValue().expireAt() < now);
    }
}
