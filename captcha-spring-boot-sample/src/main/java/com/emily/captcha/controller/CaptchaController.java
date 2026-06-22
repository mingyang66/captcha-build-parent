package com.emily.captcha.controller;

import com.emily.captcha.click.model.ClickCaptcha;
import com.emily.captcha.click.model.ClickPoint;
import com.emily.captcha.click.service.ClickCaptchaService;
import com.emily.captcha.slider.model.SliderCaptcha;
import com.emily.captcha.slider.service.SliderCaptchaService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 验证码 REST 接口
 */
@RestController
@RequestMapping("/captcha")
public class CaptchaController {

    private final ClickCaptchaService captchaService;
    private final SliderCaptchaService sliderCaptchaService;

    public CaptchaController(ClickCaptchaService captchaService, SliderCaptchaService sliderCaptchaService) {
        this.captchaService = captchaService;
        this.sliderCaptchaService = sliderCaptchaService;
    }

    /**
     * 获取一个新的验证码
     * GET /captcha/generate
     */
    @GetMapping("/generate")
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
    @PostMapping("/verify")
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
    @GetMapping("/slider/generate")
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
    @PostMapping("/slider/verify")
    public Map<String, Object> sliderVerify(@RequestBody SliderVerifyRequest request) {
        boolean success = sliderCaptchaService.verify(request.getCaptchaId(), request.getX());

        Map<String, Object> result = new HashMap<>();
        result.put("code", success ? 200 : 400);
        result.put("message", success ? "验证通过" : "验证失败，请重新滑动");
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
}
