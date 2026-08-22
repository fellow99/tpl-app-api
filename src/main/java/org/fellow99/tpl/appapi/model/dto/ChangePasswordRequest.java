package org.fellow99.tpl.appapi.model.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 修改密码请求
 *
 * <p>字段：当前密码、新密码、确认新密码、短信验证码、图形验证码。</p>
 */
@Data
public class ChangePasswordRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String oldPassword;

    private String newPassword;

    private String confirmPassword;

    private String smsCode;

    private String code;

    private String uuid;
}
