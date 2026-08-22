package org.fellow99.tpl.appapi.service;

import org.fellow99.tpl.appapi.model.MessageKey;
import cn.hutool.core.util.StrUtil;
import org.fellow99.tpl.appapi.util.CaptchaUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 图形验证码服务：生成、校验（供注册/登录/修改信息/修改密码复用）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CaptchaService {

    private final StringRedisTemplate stringRedisTemplate;

    @Value("${captcha.enable:true}")
    private boolean captchaEnabled;

    private static final String CAPTCHA_REDIS_KEY = "captcha_codes:{}";
    private static final int CAPTCHA_TTL_MINUTES = 5;

    public boolean isEnabled() {
        return captchaEnabled;
    }

    public Map<String, Object> generateCaptcha() {
        Map<String, Object> map = new HashMap<>();
        map.put("captchaEnabled", captchaEnabled);

        if (captchaEnabled) {
            CaptchaUtils.CaptchaResult result = CaptchaUtils.generate();
            String key = CAPTCHA_REDIS_KEY.replace("{}", result.getUuid());
            stringRedisTemplate.opsForValue().set(key, result.getCode(), CAPTCHA_TTL_MINUTES, TimeUnit.MINUTES);
            map.put("uuid", result.getUuid());
            map.put("img", result.getImg());
        } else {
            map.put("uuid", "");
            map.put("img", "");
        }
        return map;
    }

    /**
     * 校验图形验证码（一次性消费）
     *
     * @param uuid 验证码唯一标识
     * @param code 用户输入
     */
    public void validate(String uuid, String code) {
        if (!captchaEnabled) {
            return;
        }
        if (StrUtil.isBlank(uuid) || StrUtil.isBlank(code)) {
            throw new IllegalArgumentException(MessageKey.CAPTCHA_REQUIRED);
        }
        String key = CAPTCHA_REDIS_KEY.replace("{}", uuid);
        String storedCode = stringRedisTemplate.opsForValue().get(key);
        if (storedCode == null) {
            throw new IllegalArgumentException(MessageKey.CAPTCHA_EXPIRED);
        }
        if (!code.equalsIgnoreCase(storedCode)) {
            throw new IllegalArgumentException(MessageKey.CAPTCHA_INCORRECT);
        }
        stringRedisTemplate.delete(key);
    }
}
