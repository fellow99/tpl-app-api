package org.fellow99.tpl.appapi.model.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 注册请求（手机注册）
 *
 * <p>手机号即用户名，存入 {@code sys_user} 的 user_name、phone_number、nick_name。</p>
 */
@Data
public class RegisterRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String phoneNumber;

    private String password;

    private String smsCode;

    private String code;

    private String uuid;
}
