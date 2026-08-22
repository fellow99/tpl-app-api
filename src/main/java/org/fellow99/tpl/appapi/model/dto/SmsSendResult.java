package org.fellow99.tpl.appapi.model.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 短信发送结果
 *
 * <p>对应 Spug Push 短信接口响应体。</p>
 */
@Data
public class SmsSendResult implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 成功状态码 */
    public static final int SUCCESS_CODE = 200;

    /** 响应码，200 表示发送成功 */
    private int code;

    /** 响应消息，成功时为"请求成功"，失败时说明具体原因 */
    private String msg;

    /** 请求 ID，用于查询发送状态 */
    private String requestId;

    /**
     * 是否发送成功
     *
     * @return code == 200
     */
    public boolean isSuccess() {
        return code == SUCCESS_CODE;
    }
}
