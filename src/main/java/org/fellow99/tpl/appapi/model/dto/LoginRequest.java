package org.fellow99.tpl.appapi.model.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 登录请求（统一登录）
 *
 * <p>{@code grantType} 区分登录方式：{@code password} 密码登录、{@code sms} 短信登录。</p>
 */
@Data
public class LoginRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String grantType;

    private String phoneNumber;

    private String password;

    private String smsCode;

    private String code;

    private String uuid;
}
