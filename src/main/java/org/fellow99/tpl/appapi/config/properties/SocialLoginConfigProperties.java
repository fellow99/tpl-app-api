package org.fellow99.tpl.appapi.config.properties;

import lombok.Data;

import java.util.List;

/**
 * 单个社交平台的登录配置。
 *
 * <p>对应 RuoYi-Vue-Plus 的 {@code SocialLoginConfigProperties}，仅保留微信接入所需字段。</p>
 */
@Data
public class SocialLoginConfigProperties {

    /** 应用 ID（AppID） */
    private String clientId;

    /** 应用密钥（AppSecret） */
    private String clientSecret;

    /** 回调地址（网站应用；移动端/小程序置空） */
    private String redirectUri;

    /** 请求范围 */
    private List<String> scopes;
}
