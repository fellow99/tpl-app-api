package org.fellow99.tpl.appapi.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 用户信息 VO（不含 password，杜绝密码哈希外泄）
 *
 * <p>供「我的」栏目展示与 {@code GET /system/user/getInfo} 返回。</p>
 */
@Data
public class UserInfoVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long userId;

    private String userName;

    private String nickName;

    private String phoneNumber;

    private String email;

    private LocalDate birthDate;
}
