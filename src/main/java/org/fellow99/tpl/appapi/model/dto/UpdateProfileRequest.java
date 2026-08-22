package org.fellow99.tpl.appapi.model.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 修改用户信息请求
 *
 * <p>字段：昵称、email、生日、图形验证码。</p>
 */
@Data
public class UpdateProfileRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String nickName;

    private String email;

    private LocalDate birthDate;

    private String code;

    private String uuid;
}
