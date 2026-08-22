package org.fellow99.tpl.appapi.model.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 第三方平台登录请求对象（网站扫码 / 移动拉起）。
 */
@Data
public class SocialLoginBody implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 客户端标识（透传，不校验） */
    private String clientId;

    /** 第三方登录平台：wechat_open / wechat_app */
    private String source;

    /** 第三方登录 code */
    private String socialCode;

    /** 第三方登录 state */
    private String socialState;
}
