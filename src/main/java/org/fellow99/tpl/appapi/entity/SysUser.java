package org.fellow99.tpl.appapi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("sys_user")
public class SysUser implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long userId;

    private String userName;

    private String nickName;

    @TableField("phone_number")
    private String phoneNumber;

    private String email;

    private String password;

    private Long avatar;

    private String status;

    private String delFlag;

    private String loginIp;

    private LocalDateTime loginDate;

    private LocalDateTime createTime;

    private Long createBy;

    private Long updateBy;

    private LocalDateTime updateTime;

    private String remark;
}
