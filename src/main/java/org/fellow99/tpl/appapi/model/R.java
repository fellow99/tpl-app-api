package org.fellow99.tpl.appapi.model;

import lombok.Data;

import java.io.Serializable;

@Data
public class R<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    private int code;

    /** 前端展示的 i18n 消息 key（见 {@link MessageKey}），前端据此到语料取对应语言文案。 */
    private String msg;

    /** 带参消息的插值参数（可选，与 msg 的 {name} 占位符对应）。 */
    private String[] msgArgs = new String[0];

    private T data;

    public static <T> R<T> ok(T data) {
        R<T> r = new R<>();
        r.code = 200;
        r.msg = MessageKey.SUCCESS;
        r.data = data;
        return r;
    }

    public static <T> R<T> ok() {
        return ok(null);
    }

    public static <T> R<T> fail(String msg) {
        R<T> r = new R<>();
        r.code = 500;
        r.msg = msg;
        r.data = null;
        return r;
    }

    public static <T> R<T> fail(int code, String msg) {
        R<T> r = new R<>();
        r.code = code;
        r.msg = msg;
        r.data = null;
        return r;
    }

    /**
     * 失败响应（带自定义业务码 + 数据负载）。
     *
     * <p>用于社交登录的「未绑定 / 需要手机号」等非错误但需要特殊业务码 + 数据的场景。</p>
     */
    public static <T> R<T> fail(int code, String msg, T data) {
        R<T> r = new R<>();
        r.code = code;
        r.msg = msg;
        r.data = data;
        return r;
    }
}
