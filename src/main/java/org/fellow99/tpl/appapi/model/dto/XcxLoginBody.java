package org.fellow99.tpl.appapi.model.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 小程序登录请求对象。
 */
@Data
public class XcxLoginBody implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 客户端标识（透传，不校验） */
    private String clientId;

    /** 小程序 wx.login 获取的 code */
    private String xcxCode;

    /** getPhoneNumber 获取的 code（首次绑定时必填） */
    private String phoneCode;
}
