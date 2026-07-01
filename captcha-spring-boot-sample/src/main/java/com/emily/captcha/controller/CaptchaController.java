package com.emily.captcha.controller;

import com.emily.captcha.click.model.ClickCaptcha;
import com.emily.captcha.click.model.ClickPoint;
import com.emily.captcha.click.service.ClickCaptchaService;
import com.emily.captcha.rotate.model.RotateCaptcha;
import com.emily.captcha.rotate.service.RotateCaptchaService;
import com.emily.captcha.slider.model.SliderCaptcha;
import com.emily.captcha.slider.service.SliderCaptchaService;
import com.emily.captcha.otp.OtpGenerator;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 验证码 REST 接口
 */
@RestController
public class CaptchaController {

    private final ClickCaptchaService captchaService;
    private final SliderCaptchaService sliderCaptchaService;
    private final RotateCaptchaService rotateCaptchaService;
    private final OtpGenerator otpService;

    public CaptchaController(ClickCaptchaService captchaService,
                             SliderCaptchaService sliderCaptchaService,
                             RotateCaptchaService rotateCaptchaService,
                             OtpGenerator otpService) {
        this.captchaService = captchaService;
        this.sliderCaptchaService = sliderCaptchaService;
        this.rotateCaptchaService = rotateCaptchaService;
        this.otpService = otpService;
    }

    /**
     * 获取一个新的验证码
     * GET /captcha/generate
     */
    @GetMapping("/api/captcha/generate")
    public Map<String, Object> generate() {
        ClickCaptcha captcha = captchaService.generate();

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "success");

        Map<String, Object> data = new HashMap<>();
        data.put("captchaId", captcha.getCaptchaId());
        data.put("image", captcha.getImage());
        data.put("promptText", captcha.getPromptText());
        // 注意：targetChars 仅供调试使用，生产环境应移除
        data.put("targetChars", captcha.getTargetChars());
        result.put("data", data);
        return result;
    }

    /**
     * 校验用户点击
     * POST /captcha/verify
     * Body: { "captchaId": "xxx", "clicks": [{"x":100,"y":80}, ...] }
     */
    @PostMapping("/api/captcha/verify")
    public Map<String, Object> verify(@RequestBody VerifyRequest request) {
        boolean success = captchaService.verify(request.getCaptchaId(), request.getClicks());

        Map<String, Object> result = new HashMap<>();
        result.put("code", success ? 200 : 400);
        result.put("message", success ? "验证通过" : "验证失败，请重新点击");
        return result;
    }

    // ==================== 滑动验证码接口 ====================

    /**
     * 获取一个新的滑动验证码
     * GET /captcha/slider/generate
     */
    @GetMapping("/api/captcha/slider/generate")
    public Map<String, Object> sliderGenerate() {
        SliderCaptcha captcha = sliderCaptchaService.generate();

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "success");

        Map<String, Object> data = new HashMap<>();
        data.put("captchaId", captcha.getCaptchaId());
        data.put("backgroundImage", captcha.getBackgroundImage());
        data.put("sliderImage", captcha.getSliderImage());
        data.put("y", captcha.getY());
        result.put("data", data);
        return result;
    }

    /**
     * 校验用户滑动位置
     * POST /captcha/slider/verify
     * Body: { "captchaId": "xxx", "x": 150 }
     */
    @PostMapping("/api/captcha/slider/verify")
    public Map<String, Object> sliderVerify(@RequestBody SliderVerifyRequest request) {
        boolean success = sliderCaptchaService.verify(request.getCaptchaId(), request.getX());

        Map<String, Object> result = new HashMap<>();
        result.put("code", success ? 200 : 400);
        result.put("message", success ? "验证通过" : "验证失败，请重新滑动");
        return result;
    }

    // ==================== 旋转验证码接口 ====================

    /**
     * 获取一个新的旋转验证码
     * GET /captcha/rotate/generate
     */
    @GetMapping("/api/captcha/rotate/generate")
    public Map<String, Object> rotateGenerate() {
        RotateCaptcha captcha = rotateCaptchaService.generate();

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "success");

        Map<String, Object> data = new HashMap<>();
        data.put("captchaId", captcha.getCaptchaId());
        data.put("image", captcha.getImage());
        result.put("data", data);
        return result;
    }

    /**
     * 校验用户旋转角度
     * POST /captcha/rotate/verify
     * Body: { "captchaId": "xxx", "angle": 180 }
     */
    @PostMapping("/api/captcha/rotate/verify")
    public Map<String, Object> rotateVerify(@RequestBody RotateVerifyRequest request) {
        boolean success = rotateCaptchaService.verify(request.getCaptchaId(), request.getAngle());

        Map<String, Object> result = new HashMap<>();
        result.put("code", success ? 200 : 400);
        result.put("message", success ? "验证通过" : "验证失败，请重新旋转");
        return result;
    }

    // ==================== OTP一次性密码接口 ====================

    /**
     * 为用户生成OTP密钥
     * POST /api/captcha/otp/secret?account=user@example.com
     */
    @PostMapping("/api/captcha/otp/secret")
    public Map<String, Object> generateOtpSecret(@RequestParam String account) {
        String secret = otpService.generateSecret(account);
        String otpAuthUri = otpService.generateOtpAuthUri(account, "LIME");

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "success");

        Map<String, Object> data = new HashMap<>();
        data.put("account", account);
        data.put("secret", secret);
        data.put("otpAuthUri", otpAuthUri);
        result.put("data", data);
        return result;
    }

    /**
     * 获取用户的OTP密钥
     * GET /api/captcha/otp/getOtpSecret?account=user@example.com
     */
    @GetMapping("/api/captcha/otp/getOtpSecret")
    public Map<String, Object> getOtpSecret(@RequestParam String account) {
        String secret = otpService.getSecret(account);

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "success");

        Map<String, Object> data = new HashMap<>();
        data.put("account", account);
        data.put("secret", secret);
        data.put("enabled", secret != null);
        result.put("data", data);
        return result;
    }

    /**
     * 验证OTP密码
     * POST /api/captcha/otp/verify
     * Body: { "account": "user@example.com", "otp": "123456" }
     */
    @PostMapping("/api/captcha/otp/verify")
    public Map<String, Object> verifyOtp(@RequestBody OtpVerifyRequest request) {
        boolean success = otpService.verify(request.getAccount(), request.getOtp());

        Map<String, Object> result = new HashMap<>();
        result.put("code", success ? 200 : 400);
        result.put("message", success ? "验证通过" : "验证失败");
        return result;
    }

    /**
     * 检查用户是否已启用OTP
     * GET /api/captcha/otp/enabled?account=user@example.com
     */
    @GetMapping("/api/captcha/otp/enabled")
    public Map<String, Object> checkOtpEnabled(@RequestParam String account) {
        boolean enabled = otpService.isEnabled(account);

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "success");

        Map<String, Object> data = new HashMap<>();
        data.put("account", account);
        data.put("enabled", enabled);
        result.put("data", data);
        return result;
    }

    /**
     * 删除用户的OTP配置
     * DELETE /api/captcha/otp?account=user@example.com
     */
    @PostMapping("/api/captcha/otp/remove")
    public Map<String, Object> removeOtp(@RequestParam String account) {
        otpService.remove(account);

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "OTP已删除");
        return result;
    }

    /**
     * 校验请求体
     */
    public static class VerifyRequest {
        private String captchaId;
        private List<ClickPoint> clicks;

        public String getCaptchaId() {
            return captchaId;
        }

        public void setCaptchaId(String captchaId) {
            this.captchaId = captchaId;
        }

        public List<ClickPoint> getClicks() {
            return clicks;
        }

        public void setClicks(List<ClickPoint> clicks) {
            this.clicks = clicks;
        }
    }

    /**
     * 滑动验证码校验请求体
     */
    public static class SliderVerifyRequest {
        private String captchaId;
        private int x;

        public String getCaptchaId() {
            return captchaId;
        }

        public void setCaptchaId(String captchaId) {
            this.captchaId = captchaId;
        }

        public int getX() {
            return x;
        }

        public void setX(int x) {
            this.x = x;
        }
    }

    /**
     * 旋转验证码校验请求体
     */
    public static class RotateVerifyRequest {
        private String captchaId;
        private int angle;

        public String getCaptchaId() {
            return captchaId;
        }

        public void setCaptchaId(String captchaId) {
            this.captchaId = captchaId;
        }

        public int getAngle() {
            return angle;
        }

        public void setAngle(int angle) {
            this.angle = angle;
        }
    }

    /**
     * OTP验证码校验请求体
     */
    public static class OtpVerifyRequest {
        private String account;
        private String otp;

        public String getAccount() {
            return account;
        }

        public void setAccount(String account) {
            this.account = account;
        }

        public String getOtp() {
            return otp;
        }

        public void setOtp(String otp) {
            this.otp = otp;
        }
    }
}
