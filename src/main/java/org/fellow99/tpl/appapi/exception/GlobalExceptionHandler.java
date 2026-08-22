package org.fellow99.tpl.appapi.exception;

import org.fellow99.tpl.appapi.model.MessageKey;
import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import cn.hutool.core.util.StrUtil;
import org.fellow99.tpl.appapi.model.R;
import lombok.extern.slf4j.Slf4j;
import me.zhyd.oauth.exception.AuthException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器。
 *
 * <p>统一将 Controller / Interceptor（SaInterceptor）/ Service 抛出的异常转换为
 * {@link R} 响应，保证响应体始终携带 {@code msg} 字段，避免前端 {@code request.ts}
 * 因拿不到 {@code msg} 而回退到误导性的「服务器繁忙，请稍后再试」。</p>
 *
 * <p>响应约定（与 constitution.md 原则二/三对齐，并做最小必要扩展）：</p>
 * <ul>
 *   <li>业务异常（{@link IllegalArgumentException} / {@link IllegalStateException} /
 *       {@link AuthException}）→ HTTP 200 + {@code R.fail(msg)}（body.code=500）；</li>
 *   <li>登录态异常（{@link NotLoginException}）→ HTTP 401，前端据状态码清除本地 token 并跳登录页；</li>
 *   <li>权限异常 → HTTP 403（当前无 RBAC，防御性预留）；</li>
 *   <li>未知系统异常 → HTTP 500 + 通用提示，完整堆栈仅记日志、不外泄。</li>
 * </ul>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Sa-Token 未登录 / 登录过期（SaInterceptor 的 checkLogin 抛出）。
     * 前端 request.ts 据 HTTP 401 清除本地 token 并跳转登录页（FR-002-020）。
     */
    @ExceptionHandler(NotLoginException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public R<Void> handleNotLogin(NotLoginException e) {
        log.warn("未登录访问被拦截: {}", e.getMessage());
        return R.fail(401, MessageKey.UNAUTHORIZED);
    }

    /**
     * Sa-Token 无权限 / 无角色（防御性预留，当前无 RBAC）。
     */
    @ExceptionHandler({NotPermissionException.class, NotRoleException.class})
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public R<Void> handleNoPermission(Exception e) {
        log.warn("无权限访问被拦截: {}", e.getMessage());
        return R.fail(403, MessageKey.FORBIDDEN);
    }

    /**
     * 微信授权异常（jscode2session 失败、AppID/Secret 缺失或错误等）。
     * 返回真实原因，便于前端展示具体错误而非通用「服务器繁忙」。
     */
    @ExceptionHandler(AuthException.class)
    public R<Void> handleAuthException(AuthException e) {
        log.warn("第三方授权失败: {}", e.getMessage());
        String msg = StrUtil.isBlank(e.getMessage()) ? MessageKey.WECHAT_LOGIN_FAILED : e.getMessage();
        return R.fail(msg);
    }

    /**
     * 业务校验异常兜底（Service 抛出的 IllegalArgumentException）。
     * Controller 已捕获时不会走到此处，此处覆盖未捕获的场景。
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public R<Void> handleIllegalArgument(IllegalArgumentException e) {
        return R.fail(e.getMessage());
    }

    /**
     * 短信发送等服务调用失败（SmsService 抛出 IllegalStateException）。
     */
    @ExceptionHandler(IllegalStateException.class)
    public R<Void> handleIllegalState(IllegalStateException e) {
        log.warn("服务调用失败: {}", e.getMessage());
        return R.fail(e.getMessage());
    }

    /**
     * 请求体解析失败（非法 JSON、字段类型不匹配等）。
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public R<Void> handleNotReadable(HttpMessageNotReadableException e) {
        log.warn("请求体解析失败: {}", e.getMessage());
        return R.fail(MessageKey.BAD_REQUEST);
    }

    /**
     * 兜底：未知系统异常。完整堆栈记日志，返回通用提示（不泄露内部信息）。
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public R<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return R.fail(MessageKey.SERVER_BUSY);
    }
}
