package com.emily.captcha.otp;

/**
 * OTP会话存储服务接口
 */
public interface OtpStoreService {

    /**
     * 存储OTP会话
     *
     * @param account 账户标识
     * @param session OTP会话数据
     */
    void put(String account, OtpSession session);

    /**
     * 获取OTP会话
     *
     * @param account 账户标识
     * @return OTP会话数据，如果不存在则返回null
     */
    OtpSession get(String account);

    /**
     * 更新OTP会话
     *
     * @param account 账户标识
     * @param session OTP会话数据
     */
    void update(String account, OtpSession session);

    /**
     * 删除OTP会话
     *
     * @param account 账户标识
     */
    void remove(String account);
}
