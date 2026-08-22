package org.fellow99.tpl.appapi.service.strategy;

import org.fellow99.tpl.appapi.model.MessageKey;
import org.fellow99.tpl.appapi.config.ApplicationContextHolder;
import org.fellow99.tpl.appapi.model.dto.SocialLoginResult;
import org.springframework.context.ApplicationContext;

/**
 * 授权策略接口。
 *
 * <p>静态 {@link #login(String, String)} 按 {@code grantType + "AuthStrategy"} 的 bean 名
 * 从 Spring 容器获取对应策略并分发（对齐 RuoYi-Vue-Plus 的 {@code IAuthStrategy}）。</p>
 */
public interface IAuthStrategy {

    String BASE_NAME = "AuthStrategy";

    /**
     * 统一登录分发。
     *
     * @param body      登录请求体（原始 JSON 字符串）
     * @param grantType 授权类型（social / xcx）
     * @return 登录结果
     */
    static SocialLoginResult login(String body, String grantType) {
        String beanName = grantType + BASE_NAME;
        ApplicationContext context = ApplicationContextHolder.getContext();
        if (context == null || !context.containsBean(beanName)) {
            throw new IllegalArgumentException(MessageKey.UNSUPPORTED_GRANT_TYPE);
        }
        IAuthStrategy strategy = context.getBean(beanName, IAuthStrategy.class);
        return strategy.login(body);
    }

    /**
     * 当前策略完成认证后的登录结果。
     *
     * @param body 登录请求体（原始 JSON 字符串）
     */
    SocialLoginResult login(String body);
}
