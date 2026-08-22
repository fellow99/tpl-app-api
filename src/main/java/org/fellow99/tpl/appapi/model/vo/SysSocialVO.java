package org.fellow99.tpl.appapi.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 第三方绑定列表 VO（已剔除 accessToken / refreshToken 等敏感字段，避免泄露 session_key）。
 */
@Data
public class SysSocialVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long userId;

    private String authId;

    private String source;

    private String openId;

    private String nickName;

    private String avatar;

    private String unionId;

    private LocalDateTime createTime;
}
