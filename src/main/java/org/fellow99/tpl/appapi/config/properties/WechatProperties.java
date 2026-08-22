package org.fellow99.tpl.appapi.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 微信登录配置属性。
 *
 * <p>对应 RuoYi-Vue-Plus 的 {@code SocialProperties}，绑定 {@code application.yml} 中的
 * {@code justauth.*} 配置项；{@code type} 的 key 为平台标识（wechat_open / wechat_app / wechat_xcx）。</p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "justauth")
public class WechatProperties {

    /**
     * 授权类型（key 为平台标识）
     */
    private Map<String, SocialLoginConfigProperties> type;
}
