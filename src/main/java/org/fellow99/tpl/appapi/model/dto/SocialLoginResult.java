package org.fellow99.tpl.appapi.model.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 社交登录结果（social / xcx 策略统一返回）。
 *
 * <p>已绑定并登录成功时 {@code bound=true} 且 {@code accessToken} 非空；
 * 未绑定（social）时 {@code bound=false} 且带 openId/unionId/nickname 供前端引导绑定；
 * 小程序首次登录未绑定且未携带 phoneCode 时 {@code needPhone=true}。</p>
 */
@Data
public class SocialLoginResult implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 已绑定并登录成功后返回的 token */
    private String accessToken;

    /** 是否已绑定（true=已登录） */
    private Boolean bound;

    /** 是否需要手机号（小程序首次登录未绑定时为 true） */
    private Boolean needPhone;

    /** 第三方来源 */
    private String source;

    /** 微信 openid */
    private String openId;

    /** unionid（若有） */
    private String unionId;

    /** 昵称 */
    private String nickname;

    /** 头像 */
    private String avatar;

    public static SocialLoginResult bound(String accessToken) {
        SocialLoginResult r = new SocialLoginResult();
        r.setBound(true);
        r.setNeedPhone(false);
        r.setAccessToken(accessToken);
        return r;
    }

    public static SocialLoginResult unbound(String source, String openId, String unionId, String nickname, String avatar) {
        SocialLoginResult r = new SocialLoginResult();
        r.setBound(false);
        r.setNeedPhone(false);
        r.setSource(source);
        r.setOpenId(openId);
        r.setUnionId(unionId);
        r.setNickname(nickname);
        r.setAvatar(avatar);
        return r;
    }

    public static SocialLoginResult needPhone(String openId, String unionId) {
        SocialLoginResult r = new SocialLoginResult();
        r.setBound(false);
        r.setNeedPhone(true);
        r.setOpenId(openId);
        r.setUnionId(unionId);
        return r;
    }
}
