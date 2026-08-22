package org.fellow99.tpl.appapi.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("tpl_user_view")
public class UserView implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long userId;

    private String userName;

    private String nickName;

    @TableField("phone_number")
    private String phoneNumber;

    private String email;

    private Long avatar;

    private String status;

    private String delFlag;

    private String loginIp;

    private LocalDateTime loginDate;

    private LocalDateTime createTime;

    private LocalDate birthDate;
}
