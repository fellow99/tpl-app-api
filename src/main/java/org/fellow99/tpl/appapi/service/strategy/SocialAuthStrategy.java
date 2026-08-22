package org.fellow99.tpl.appapi.service.strategy;

import org.fellow99.tpl.appapi.model.MessageKey;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import org.fellow99.tpl.appapi.entity.SysSocial;
import org.fellow99.tpl.appapi.entity.SysUser;
import org.fellow99.tpl.appapi.mapper.SysUserMapper;
import org.fellow99.tpl.appapi.model.dto.SocialLoginBody;
import org.fellow99.tpl.appapi.model.dto.SocialLoginResult;
import org.fellow99.tpl.appapi.service.SysSocialService;
import org.fellow99.tpl.appapi.util.WechatAuthUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.zhyd.oauth.model.AuthResponse;
import me.zhyd.oauth.model.AuthUser;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * 第三方授权策略（网站扫码 wechat_open + 移动拉起 wechat_app）。
 *
 * <p>bean 名 = {@code "social"} + {@link IAuthStrategy#BASE_NAME}。</p>
 */
@Slf4j
@Service("social" + IAuthStrategy.BASE_NAME)
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "wechat", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SocialAuthStrategy implements IAuthStrategy {

    private final WechatAuthUtils wechatAuthUtils;

    private final SysSocialService sysSocialService;

    private final SysUserMapper sysUserMapper;

    @Override
    public SocialLoginResult login(String body) {
        SocialLoginBody loginBody = JSONUtil.toBean(body, SocialLoginBody.class);
        String source = loginBody.getSource();
        String socialCode = loginBody.getSocialCode();
        String socialState = loginBody.getSocialState();
        if (StrUtil.isBlank(source) || StrUtil.isBlank(socialCode) || StrUtil.isBlank(socialState)) {
            throw new IllegalArgumentException(MessageKey.SOCIAL_PARAM_REQUIRED);
        }

        AuthResponse<AuthUser> response = wechatAuthUtils.loginAuth(source, socialCode, socialState);
        if (!response.ok()) {
            throw new IllegalArgumentException(response.getMsg());
        }
        AuthUser authUser = response.getData();

        // authId = source + openid（source 用请求参数小写键，而非 JustAuth 的 AuthUser.getSource()
        // 其返回枚举大写名如 "WECHAT_OPEN"，会导致 wechat_open/wechat_app 混同且大小写不一致）
        String authId = source + authUser.getUuid();
        SysSocial social = sysSocialService.selectByAuthId(authId);
        if (social == null) {
            // 未绑定：返回「未绑定」信号 + openid（业务码 2001 由 AuthController 映射），
            // 前端引导走「手机号主键」关联：手机号登录后绑定（POST /auth/social/callback）或注册。
            String unionId = authUser.getToken() == null ? null : authUser.getToken().getUnionId();
            return SocialLoginResult.unbound(source, authUser.getUuid(), unionId,
                authUser.getNickname(), authUser.getAvatar());
        }

        SysUser user = sysUserMapper.selectById(social.getUserId());
        if (user == null) {
            throw new IllegalArgumentException(MessageKey.SOCIAL_BOUND_USER_NOT_FOUND);
        }
        if ("1".equals(user.getStatus())) {
            throw new IllegalArgumentException(MessageKey.USER_DISABLED);
        }
        StpUtil.login(user.getUserId());
        log.info("用户第三方登录成功: userId={}, source={}", user.getUserId(), source);
        return SocialLoginResult.bound(StpUtil.getTokenValue());
    }
}
