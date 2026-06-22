package com.emily.captcha;

import com.emily.captcha.click.service.ClickCaptchaService;
import com.emily.captcha.click.store.CaptchaSessionStoreService;
import com.emily.captcha.click.store.DefaultCaptchaSessionStoreServiceImpl;
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
    public ClickCaptchaService clickCaptchaService(CaptchaProperties properties, CaptchaSessionStoreService captchaStoreService) {
        return new ClickCaptchaService(properties, captchaStoreService);
    }

    @Bean
    @ConditionalOnMissingBean
    public CaptchaSessionStoreService captchaSessionStoreService() {
        return new DefaultCaptchaSessionStoreServiceImpl();
    }

    /**
     * 定时清理过期验证码，每 60 秒执行一次
     */
    @Configuration
    @EnableScheduling
    static class CaptchaCleanupConfiguration {

        private final CaptchaSessionStoreService storeService;

        CaptchaCleanupConfiguration(CaptchaSessionStoreService storeService) {
            this.storeService = storeService;
        }

        @Scheduled(fixedDelay = 60_000)
        public void cleanExpiredCaptcha() {
            storeService.cleanExpired();
        }
    }
}
