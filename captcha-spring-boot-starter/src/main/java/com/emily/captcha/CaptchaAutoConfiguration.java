package com.emily.captcha;

import com.emily.captcha.click.service.ClickCaptchaService;
import com.emily.captcha.click.store.ClickStoreService;
import com.emily.captcha.click.store.DefaultClickStoreServiceImpl;
import com.emily.captcha.otp.OtpGenerator;
import com.emily.captcha.otp.store.DefaultOtpStoreServiceImpl;
import com.emily.captcha.otp.store.OtpStoreService;
import com.emily.captcha.rotate.service.RotateCaptchaService;
import com.emily.captcha.rotate.store.DefaultRotateStoreServiceImpl;
import com.emily.captcha.rotate.store.RotateStoreService;
import com.emily.captcha.slider.service.SliderCaptchaService;
import com.emily.captcha.slider.store.DefaultSliderStoreServiceImpl;
import com.emily.captcha.slider.store.SliderStoreService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * 验证码自动配置
 * <p>
 * 通过 {@code captcha.enabled=false} 可完全关闭验证码功能。
 */
@AutoConfiguration
@EnableConfigurationProperties(CaptchaProperties.class)
@ConditionalOnProperty(prefix = CaptchaProperties.PREFIX, name = "enabled", havingValue = "true", matchIfMissing = true)
public class CaptchaAutoConfiguration {

    @Bean
    public ClickCaptchaService clickCaptchaService(CaptchaProperties properties, ClickStoreService clickStoreService) {
        return new ClickCaptchaService(properties, clickStoreService);
    }

    @Bean
    @ConditionalOnMissingBean
    public ClickStoreService captchaSessionStoreService() {
        return new DefaultClickStoreServiceImpl();
    }

    @Bean
    public SliderCaptchaService sliderCaptchaService(CaptchaProperties properties, SliderStoreService sliderStoreService) {
        return new SliderCaptchaService(properties, sliderStoreService);
    }

    @Bean
    @ConditionalOnMissingBean
    public SliderStoreService sliderStoreService() {
        return new DefaultSliderStoreServiceImpl();
    }

    @Bean
    public RotateCaptchaService rotateCaptchaService(CaptchaProperties properties, RotateStoreService rotateStoreService) {
        return new RotateCaptchaService(properties, rotateStoreService);
    }

    @Bean
    @ConditionalOnMissingBean
    public RotateStoreService rotateStoreService() {
        return new DefaultRotateStoreServiceImpl();
    }

    @Bean
    public OtpGenerator otpService(CaptchaProperties properties, OtpStoreService otpStoreService) {
        return new OtpGenerator(properties, otpStoreService);
    }

    @Bean
    @ConditionalOnMissingBean
    public OtpStoreService otpStoreService() {
        return new DefaultOtpStoreServiceImpl();
    }

    /**
     * 定时清理过期验证码，每 60 秒执行一次
     */
    @Configuration
    @EnableScheduling
    static class CaptchaCleanupConfiguration {

        private final ClickStoreService storeService;
        private final SliderStoreService sliderStoreService;
        private final RotateStoreService rotateStoreService;
        private final OtpStoreService otpStoreService;

        CaptchaCleanupConfiguration(ClickStoreService storeService,
                                    SliderStoreService sliderStoreService,
                                    RotateStoreService rotateStoreService,
                                    OtpStoreService otpStoreService) {
            this.storeService = storeService;
            this.sliderStoreService = sliderStoreService;
            this.rotateStoreService = rotateStoreService;
            this.otpStoreService = otpStoreService;
        }

        @Scheduled(fixedDelay = 60_000)
        public void cleanExpiredCaptcha() {
            storeService.cleanExpired();
            sliderStoreService.cleanExpired();
            rotateStoreService.cleanExpired();
            otpStoreService.cleanExpired();
        }
    }
}
