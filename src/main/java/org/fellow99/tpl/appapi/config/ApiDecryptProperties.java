package org.fellow99.tpl.appapi.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 请求解密配置（AES+RSA 混合加密）
 *
 * <p>绑定 {@code application.yml} 中的 {@code api-decrypt.*} 配置项，对齐 RuoYi-Vue-Plus 的 {@code api-decrypt}。</p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "api-decrypt")
public class ApiDecryptProperties {

    /** 加解密总开关（dev 可关闭以便调试） */
    private boolean enabled = true;

    /** 携带 RSA 加密 AES 密钥的请求头名 */
    private String headerFlag = "encrypt-key";

    /** 服务端 RSA 私钥（Base64 PKCS#8），来自环境变量，禁止硬编码 */
    private String privateKey;
}
