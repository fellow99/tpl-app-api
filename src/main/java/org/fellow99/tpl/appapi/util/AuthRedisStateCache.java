package org.fellow99.tpl.appapi.util;

import lombok.RequiredArgsConstructor;
import me.zhyd.oauth.cache.AuthStateCache;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 第三方授权 state 缓存（Redis 实现）。
 *
 * <p>对应 RuoYi-Vue-Plus 的 {@code AuthRedisStateCache}，实现 {@link AuthStateCache}，
 * state 存储于 Redis，key 为 {@code tpl:social_auth_codes:{state}}，默认 3 分钟过期。</p>
 */
@Component
@RequiredArgsConstructor
public class AuthRedisStateCache implements AuthStateCache {

    private final StringRedisTemplate stringRedisTemplate;

    private static final String KEY_PREFIX = "tpl:social_auth_codes:";

    private static final long DEFAULT_TTL_MINUTES = 3;

    @Override
    public void cache(String key, String value) {
        stringRedisTemplate.opsForValue().set(KEY_PREFIX + key, value, DEFAULT_TTL_MINUTES, TimeUnit.MINUTES);
    }

    @Override
    public void cache(String key, String value, long timeout) {
        stringRedisTemplate.opsForValue().set(KEY_PREFIX + key, value, timeout, TimeUnit.MILLISECONDS);
    }

    @Override
    public String get(String key) {
        return stringRedisTemplate.opsForValue().get(KEY_PREFIX + key);
    }

    @Override
    public boolean containsKey(String key) {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(KEY_PREFIX + key));
    }
}
