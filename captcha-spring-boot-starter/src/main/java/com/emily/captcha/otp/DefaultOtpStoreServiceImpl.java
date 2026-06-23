package com.emily.captcha.otp;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于内存的OTP会话存储默认实现
 */
public class DefaultOtpStoreServiceImpl implements OtpStoreService {

    private final ConcurrentHashMap<String, OtpSession> store = new ConcurrentHashMap<>();

    @Override
    public void put(String account, OtpSession session) {
        store.put(account, session);
    }

    @Override
    public OtpSession get(String account) {
        return store.get(account);
    }

    @Override
    public void update(String account, OtpSession session) {
        store.put(account, session);
    }

    @Override
    public void remove(String account) {
        store.remove(account);
    }
}
