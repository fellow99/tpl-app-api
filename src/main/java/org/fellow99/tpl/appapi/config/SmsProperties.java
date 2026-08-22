package org.fellow99.tpl.appapi.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 短信平台配置（Spug Push）
 *
 * <p>绑定 {@code application.yml} 中的 {@code sms.*} 配置项。</p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "sms")
public class SmsProperties {

    /** 短信平台基础地址 */
    private String baseUrl = "https://push.spug.cc";

    /** 短信模板编码（调用凭证，由平台生成） */
    private String templateCode;

    /** 连接超时（毫秒） */
    private int connectTimeout = 5000;

    /** 读取超时（毫秒） */
    private int readTimeout = 10000;
}
