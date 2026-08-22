package org.fellow99.tpl.appapi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 社会化关系对象 sys_social。
 *
 * <p>字段与 {@code sql/001-init-sys.sql} 中 {@code sys_social} 表完全对应（零 DDL 变更）。</p>
 */
@Data
@TableName("sys_social")
public class SysSocial implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 平台+平台唯一id */
    private String authId;

    /** 用户来源 */
    private String source;

    /** 平台编号唯一id（openid） */
    private String openId;

    /** 登录账号 */
    private String userName;

    /** 用户昵称 */
    private String nickName;

    /** 用户邮箱 */
    private String email;

    /** 头像地址 */
    private String avatar;

    /** 用户的授权令牌（小程序流程下存 session_key，仅服务端保存） */
    private String accessToken;

    /** 用户的授权令牌的有效期，部分平台可能没有 */
    private Long expireIn;

    /** 刷新令牌，部分平台可能没有 */
    private String refreshToken;

    /** 平台的授权信息，部分平台可能没有 */
    private String accessCode;

    /** 用户的 unionid */
    private String unionId;

    /** 授予的权限，部分平台可能没有 */
    private String scope;

    /** 个别平台的授权信息，部分平台可能没有 */
    private String tokenType;

    /** id token，部分平台可能没有 */
    private String idToken;

    /** 小米平台用户的附带属性，部分平台可能没有 */
    private String macAlgorithm;

    /** 小米平台用户的附带属性，部分平台可能没有 */
    private String macKey;

    /** 用户的授权code，部分平台可能没有 */
    private String code;

    /** Twitter平台用户的附带属性，部分平台可能没有 */
    private String oauthToken;

    /** Twitter平台用户的附带属性，部分平台可能没有 */
    private String oauthTokenSecret;

    /** 创建部门 */
    private Long createDept;

    /** 创建者 */
    private Long createBy;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新者 */
    private Long updateBy;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 删除标志（0代表存在 1代表删除） */
    private String delFlag;
}
