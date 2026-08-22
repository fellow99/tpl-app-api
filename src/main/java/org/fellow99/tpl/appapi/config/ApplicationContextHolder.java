package org.fellow99.tpl.appapi.config;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Spring 容器持有者。
 *
 * <p>tpl-app-api 无 SpringUtils 工具类，此处提供轻量静态访问入口，供
 * {@code IAuthStrategy} 等静态分发方法按 bean 名获取策略 Bean。</p>
 */
@Component
public class ApplicationContextHolder implements ApplicationContextAware {

    private static ApplicationContext context;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        context = applicationContext;
    }

    public static ApplicationContext getContext() {
        return context;
    }
}
