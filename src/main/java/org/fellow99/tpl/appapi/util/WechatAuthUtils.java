package org.fellow99.tpl.appapi.util;

import org.fellow99.tpl.appapi.model.MessageKey;
import org.fellow99.tpl.appapi.config.properties.SocialLoginConfigProperties;
import org.fellow99.tpl.appapi.config.properties.WechatProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.zhyd.oauth.config.AuthConfig;
import me.zhyd.oauth.exception.AuthException;
import me.zhyd.oauth.model.AuthCallback;
import me.zhyd.oauth.model.AuthResponse;
import me.zhyd.oauth.model.AuthUser;
import me.zhyd.oauth.request.AuthRequest;
import me.zhyd.oauth.request.AuthWeChatOpenRequest;
import me.zhyd.oauth.request.AuthWechatMiniProgramRequest;
import me.zhyd.oauth.utils.AuthStateUtils;
import org.springframework.stereotype.Component;

/**
 * 微信授权工具类。
 *
 * <p>对应 RuoYi-Vue-Plus 的 {@code SocialUtils}，仅保留微信相关 source：
 * {@code wechat_open} / {@code wechat_app} → {@link AuthWeChatOpenRequest}，
 * {@code wechat_xcx} → {@link AuthWechatMiniProgramRequest}（jscode2session）。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WechatAuthUtils {

    private final WechatProperties wechatProperties;

    private final AuthRedisStateCache stateCache;

    /**
     * 执行第三方登录授权回调。
     *
     * @param source 平台标识（wechat_open / wechat_app / wechat_xcx）
     * @param code   授权码
     * @param state  状态值（小程序无 state，传 null 亦可）
     * @return 授权响应
     */
    public AuthResponse<AuthUser> loginAuth(String source, String code, String state) {
        AuthRequest authRequest = getAuthRequest(source);
        AuthCallback callback = new AuthCallback();
        callback.setCode(code);
        callback.setState(state);
        return authRequest.login(callback);
    }

    /**
     * 生成第三方授权跳转地址（仅网站扫码 wechat_open 使用）。
     */
    public String authorizeUrl(String source) {
        return getAuthRequest(source).authorize(AuthStateUtils.createState());
    }

    /**
     * 获取指定平台的授权配置（不存在返回 null）。
     */
    public SocialLoginConfigProperties configOf(String source) {
        if (wechatProperties.getType() == null) {
            return null;
        }
        return wechatProperties.getType().get(source);
    }

    /**
     * 根据平台标识构建授权请求实例。
     */
    public AuthRequest getAuthRequest(String source) {
        SocialLoginConfigProperties cfg = configOf(source);
        if (cfg == null) {
            throw new AuthException(MessageKey.WECHAT_UNSUPPORTED_SOURCE);
        }
        return switch (source) {
            case "wechat_open" -> new AuthWeChatOpenRequest(
                AuthConfig.builder()
                    .clientId(cfg.getClientId())
                    .clientSecret(cfg.getClientSecret())
                    .redirectUri(cfg.getRedirectUri())
                    .scopes(cfg.getScopes())
                    .build(),
                stateCache);
            // 移动应用：state 由客户端本地生成（无服务端 state 缓存），跳过 JustAuth 的 state 校验；
            // 客户端已在本地校验 respState == state
            case "wechat_app" -> new AuthWeChatOpenRequest(
                AuthConfig.builder()
                    .clientId(cfg.getClientId())
                    .clientSecret(cfg.getClientSecret())
                    .redirectUri(cfg.getRedirectUri())
                    .scopes(cfg.getScopes())
                    .ignoreCheckState(true)
                    .build(),
                stateCache);
            // 小程序走 jscode2session，无需回调地址与 state 校验
            case "wechat_xcx" -> new AuthWechatMiniProgramRequest(
                AuthConfig.builder()
                    .clientId(cfg.getClientId())
                    .clientSecret(cfg.getClientSecret())
                    .ignoreCheckRedirectUri(true)
                    .ignoreCheckState(true)
                    .build());
            default -> throw new AuthException(MessageKey.WECHAT_INVALID_AUTH_CONFIG);
        };
    }
}
