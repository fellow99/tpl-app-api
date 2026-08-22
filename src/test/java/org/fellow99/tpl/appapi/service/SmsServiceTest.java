package org.fellow99.tpl.appapi.service;

import org.fellow99.tpl.appapi.config.SmsProperties;
import org.fellow99.tpl.appapi.model.dto.SmsSendResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link SmsService} 单元测试：参数校验（sendVerificationCode）、缓存去重（sendCode）、消费校验（verifyCode）。
 */
class SmsServiceTest {

    private static final String PHONE = "13800000000";
    private static final String SMS_KEY = "sms_code:" + PHONE;

    private SmsProperties properties;
    private StringRedisTemplate redisTemplate;
    @SuppressWarnings("unchecked")
    private ValueOperations<String, String> valueOps;
    private SmsService smsService;

    @BeforeEach
    void setUp() {
        properties = new SmsProperties();
        properties.setTemplateCode("test-template-code");

        redisTemplate = mock(StringRedisTemplate.class);
        valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        smsService = new SmsService(properties, redisTemplate);
    }

    // ===== 参数校验（sendVerificationCode）=====

    @Test
    void invalidPhoneNonElevenDigitsShouldThrow() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> smsService.sendVerificationCode("123", "1234")
        );
        assertEquals("手机号格式不正确", ex.getMessage());
    }

    @Test
    void blankPhoneShouldThrow() {
        assertThrows(
                IllegalArgumentException.class,
                () -> smsService.sendVerificationCode(null, "1234")
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> smsService.sendVerificationCode("", "1234")
        );
    }

    @Test
    void phoneNotStartingWithOneShouldThrow() {
        assertThrows(
                IllegalArgumentException.class,
                () -> smsService.sendVerificationCode("23800000000", "1234")
        );
    }

    @Test
    void shortCodeShouldThrow() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> smsService.sendVerificationCode(PHONE, "123")
        );
        assertEquals("验证码格式不正确，应为 4-6 位数字或字母", ex.getMessage());
    }

    @Test
    void longCodeShouldThrow() {
        assertThrows(
                IllegalArgumentException.class,
                () -> smsService.sendVerificationCode(PHONE, "1234567")
        );
    }

    @Test
    void blankCodeShouldThrow() {
        assertThrows(
                IllegalArgumentException.class,
                () -> smsService.sendVerificationCode(PHONE, null)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> smsService.sendVerificationCode(PHONE, "")
        );
    }

    @Test
    void codeWithIllegalCharShouldThrow() {
        assertThrows(
                IllegalArgumentException.class,
                () -> smsService.sendVerificationCode(PHONE, "12ab!")
        );
    }

    // ===== 缓存去重（sendCode）=====

    @Test
    void sendCodeWhenCachedShouldSkipSending() {
        when(valueOps.get(SMS_KEY)).thenReturn("123456");

        SmsSendResult result = smsService.sendCode(PHONE);

        assertTrue(result.isSuccess());
        assertNull(result.getRequestId());
        // 命中缓存：不重新写入，不触发发送
        verify(valueOps, never()).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));
    }

    @Test
    void sendCodeWhenTransportFailureShouldRollbackAndThrow() {
        when(valueOps.get(anyString())).thenReturn(null);
        SmsService svc = new StubbedSmsService(properties, redisTemplate, null, new RuntimeException("connection refused"));

        assertThrows(IllegalStateException.class, () -> svc.sendCode(PHONE));
        verify(valueOps).set(eq(SMS_KEY), anyString(), eq(10L), eq(TimeUnit.MINUTES));
        // 传输失败：回滚缓存并抛出
        verify(redisTemplate).delete(SMS_KEY);
    }

    @Test
    void sendCodeWhenBusinessFailureShouldRollbackCache() {
        when(valueOps.get(anyString())).thenReturn(null);
        SmsService svc = new StubbedSmsService(properties, redisTemplate, "{\"code\":500,\"msg\":\"余额不足\"}", null);

        SmsSendResult result = svc.sendCode(PHONE);

        assertFalse(result.isSuccess());
        // 业务失败：回滚缓存
        verify(redisTemplate).delete(SMS_KEY);
    }

    @Test
    void sendCodeWhenInvalidPhoneShouldThrow() {
        assertThrows(IllegalArgumentException.class, () -> smsService.sendCode("123"));
    }

    // ===== 消费校验（verifyCode）=====

    @Test
    void verifyCodeWhenMatchShouldConsumeAndReturnTrue() {
        when(valueOps.get(SMS_KEY)).thenReturn("123456");

        assertTrue(smsService.verifyCode(PHONE, "123456"));
        verify(redisTemplate).delete(SMS_KEY);
    }

    @Test
    void verifyCodeWhenMismatchShouldReturnFalse() {
        when(valueOps.get(SMS_KEY)).thenReturn("654321");

        assertFalse(smsService.verifyCode(PHONE, "123456"));
        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    void verifyCodeWhenNoCacheShouldReturnFalse() {
        when(valueOps.get(SMS_KEY)).thenReturn(null);

        assertFalse(smsService.verifyCode(PHONE, "123456"));
    }

    @Test
    void verifyCodeWhenBlankShouldReturnFalse() {
        assertFalse(smsService.verifyCode(null, "123456"));
        assertFalse(smsService.verifyCode("", "123456"));
        assertFalse(smsService.verifyCode(PHONE, null));
        assertFalse(smsService.verifyCode(PHONE, ""));
    }

    /**
     * 测试桩：覆盖 {@link SmsService#doPost}，避免真实 HTTP 调用。
     */
    private static class StubbedSmsService extends SmsService {

        private final String responseBody;
        private final RuntimeException failure;

        StubbedSmsService(SmsProperties properties, StringRedisTemplate redisTemplate, String responseBody, RuntimeException failure) {
            super(properties, redisTemplate);
            this.responseBody = responseBody;
            this.failure = failure;
        }

        @Override
        String doPost(String url, String body) {
            if (failure != null) {
                throw failure;
            }
            return responseBody;
        }
    }
}
