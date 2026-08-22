package org.fellow99.tpl.appapi.filter;

import org.fellow99.tpl.appapi.model.MessageKey;
import cn.hutool.core.util.StrUtil;
import org.fellow99.tpl.appapi.config.ApiDecryptProperties;
import org.fellow99.tpl.appapi.util.EncryptUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 请求解密过滤器：对携带 {@code encrypt-key} 头的 POST/PUT 请求做 AES+RSA 混合解密，
 * 使后续 {@code @RequestBody} 能反序列化出明文 JSON。运行于 Spring MVC 拦截器（含 SaInterceptor）之前。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApiDecryptFilter extends OncePerRequestFilter {

    private final ApiDecryptProperties properties;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (properties.isEnabled() && needsDecrypt(request)) {
            try {
                String bodyBase64 = readBody(request);
                String encryptKey = request.getHeader(properties.getHeaderFlag());
                byte[] aesKey = EncryptUtils.decryptAesKey(encryptKey, properties.getPrivateKey());
                String plaintext = EncryptUtils.decryptBody(bodyBase64, aesKey);
                DecryptRequestBodyWrapper wrapper =
                        new DecryptRequestBodyWrapper(request, plaintext.getBytes(StandardCharsets.UTF_8));
                filterChain.doFilter(wrapper, response);
                return;
            } catch (Exception e) {
                log.error("请求解密失败", e);
                writeError(response);
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private boolean needsDecrypt(HttpServletRequest request) {
        String method = request.getMethod();
        if (!HttpMethod.POST.matches(method) && !HttpMethod.PUT.matches(method)) {
            return false;
        }
        String encryptKey = request.getHeader(properties.getHeaderFlag());
        return StrUtil.isNotBlank(encryptKey);
    }

    private String readBody(HttpServletRequest request) throws IOException {
        byte[] bytes = request.getInputStream().readAllBytes();
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private void writeError(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":500,\"msg\":\"" + MessageKey.DECRYPT_FAILED + "\",\"data\":null}");
    }
}
