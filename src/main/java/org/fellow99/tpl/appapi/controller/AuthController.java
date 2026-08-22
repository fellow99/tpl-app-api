package org.fellow99.tpl.appapi.controller;

import org.fellow99.tpl.appapi.model.MessageKey;
import cn.dev33.satoken.annotation.SaIgnore;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.fellow99.tpl.appapi.model.R;
import org.fellow99.tpl.appapi.config.properties.SocialLoginConfigProperties;
import org.fellow99.tpl.appapi.model.dto.LoginRequest;
import org.fellow99.tpl.appapi.model.dto.LoginResponse;
import org.fellow99.tpl.appapi.model.dto.RegisterRequest;
import org.fellow99.tpl.appapi.model.dto.SocialLoginBody;
import org.fellow99.tpl.appapi.model.dto.SocialLoginResult;
import org.fellow99.tpl.appapi.service.AuthService;
import org.fellow99.tpl.appapi.service.SysSocialService;
import org.fellow99.tpl.appapi.service.strategy.IAuthStrategy;
import org.fellow99.tpl.appapi.util.WechatAuthUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.zhyd.oauth.model.AuthResponse;
import me.zhyd.oauth.model.AuthUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    private final WechatAuthUtils wechatAuthUtils;

    private final SysSocialService sysSocialService;

    @Value("${wechat.enabled:true}")
    private boolean wechatEnabled;

    /** 业务码：微信未绑定账号（前端引导手机号登录后绑定） */
    private static final int CODE_UNBOUND = 2001;

    /** 业务码：小程序首次登录需手机号 */
    private static final int CODE_NEED_PHONE = 2002;

    @SaIgnore
    @PostMapping("/register")
    public R<LoginResponse> register(@RequestBody RegisterRequest req) {
        try {
            LoginResponse resp = authService.register(req);
            return R.ok(resp);
        } catch (IllegalArgumentException e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 统一登录：{@code grantType=password/sms} 走原 {@link AuthService#login}，
     * {@code grantType=social/xcx} 经 {@link IAuthStrategy} 策略分发。
     *
     * <p>请求体改为原始 JSON 字符串，便于 social/xcx 携带不同字段；password/sms 反序列化为
     * {@link LoginRequest}，行为不变。</p>
     */
    @SaIgnore
    @PostMapping("/login")
    public R<?> login(@RequestBody String body) {
        try {
            if (StrUtil.isBlank(body)) {
                throw new IllegalArgumentException(MessageKey.REQUEST_BODY_EMPTY);
            }
            JSONObject json = JSONUtil.parseObj(body);
            String grantType = json.getStr("grantType", "password");
            if ("social".equals(grantType) || "xcx".equals(grantType)) {
                if (!wechatEnabled) {
                    throw new IllegalArgumentException(MessageKey.WECHAT_NOT_ENABLED);
                }
                SocialLoginResult result = IAuthStrategy.login(body, grantType);
                if (Boolean.TRUE.equals(result.getBound())) {
                    return R.ok(new LoginResponse(result.getAccessToken()));
                }
                if (Boolean.TRUE.equals(result.getNeedPhone())) {
                    return R.fail(CODE_NEED_PHONE, MessageKey.WECHAT_UNBOUND_NEED_PHONE, result);
                }
                return R.fail(CODE_UNBOUND, MessageKey.WECHAT_UNBOUND_USE_PHONE_LOGIN, result);
            }
            LoginRequest req = JSONUtil.toBean(body, LoginRequest.class);
            LoginResponse resp = authService.login(req);
            return R.ok(resp);
        } catch (IllegalArgumentException e) {
            return R.fail(e.getMessage());
        }
    }

    @PostMapping("/logout")
    public R<Void> logout() {
        StpUtil.logout();
        return R.ok();
    }

    @SaIgnore
    @GetMapping("/code")
    public R<Map<String, Object>> getCode() {
        Map<String, Object> captcha = authService.generateCaptcha();
        return R.ok(captcha);
    }

    /**
     * 获取第三方绑定跳转地址（网站扫码 wechat_open 使用）。
     */
    @SaIgnore
    @GetMapping("/binding/{source}")
    public R<String> authBinding(@PathVariable("source") String source) {
        if (!wechatEnabled) {
            return R.fail(MessageKey.WECHAT_NOT_ENABLED);
        }
        // 仅网站扫码（wechat_open）需要授权 URL；移动端直接拉起、小程序用 wx.login
        if (!"wechat_open".equals(source)) {
            return R.fail(MessageKey.WECHAT_PLATFORM_UNSUPPORTED);
        }
        SocialLoginConfigProperties cfg = wechatAuthUtils.configOf(source);
        if (cfg == null || StrUtil.isBlank(cfg.getClientId())) {
            return R.fail(MessageKey.WECHAT_PLATFORM_NOT_CONFIGURED);
        }
        return R.ok(wechatAuthUtils.authorizeUrl(source));
    }

    /**
     * 已登录用户绑定第三方（个人中心「绑定微信」）。
     */
    @PostMapping("/social/callback")
    public R<Void> socialCallback(@RequestBody SocialLoginBody loginBody) {
        try {
            StpUtil.checkLogin();
            if (!wechatEnabled) {
                throw new IllegalArgumentException(MessageKey.WECHAT_NOT_ENABLED);
            }
            AuthResponse<AuthUser> response = wechatAuthUtils.loginAuth(
                loginBody.getSource(), loginBody.getSocialCode(), loginBody.getSocialState());
            if (!response.ok()) {
                throw new IllegalArgumentException(response.getMsg());
            }
            sysSocialService.bindSocial(StpUtil.getLoginIdAsLong(), loginBody.getSource(), response.getData());
            return R.ok();
        } catch (IllegalArgumentException e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 解除第三方绑定。
     */
    @DeleteMapping("/unlock/{socialId}")
    public R<Void> unlockSocial(@PathVariable Long socialId) {
        try {
            StpUtil.checkLogin();
            if (!wechatEnabled) {
                throw new IllegalArgumentException(MessageKey.WECHAT_NOT_ENABLED);
            }
            boolean rows = sysSocialService.deleteById(socialId);
            return rows ? R.ok() : R.fail(MessageKey.UNBIND_FAILED);
        } catch (IllegalArgumentException e) {
            return R.fail(e.getMessage());
        }
    }
}
