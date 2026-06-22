package com.emily.captcha.click.store;

import com.emily.captcha.click.model.ClickPoint;

import java.util.List;

public class CaptchaSession {
    private final List<String> targetChars;
    private final List<ClickPoint> targetPoints;
    private final long expireAt;

    public CaptchaSession(List<String> targetChars, List<ClickPoint> targetPoints, long expireAt) {
        this.targetChars = targetChars;
        this.targetPoints = targetPoints;
        this.expireAt = expireAt;
    }

    public List<String> getTargetChars() {
        return targetChars;
    }

    public List<ClickPoint> getTargetPoints() {
        return targetPoints;
    }

    public long getExpireAt() {
        return expireAt;
    }
}
