package org.fellow99.tpl.appapi.util;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.asymmetric.KeyType;
import cn.hutool.crypto.asymmetric.RSA;
import cn.hutool.crypto.symmetric.AES;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 加解密工具类（AES+RSA 混合加密，对齐 RuoYi-Vue-Plus）
 *
 * <p>wire 契约（跨端统一）：</p>
 * <ul>
 *   <li>对称算法：AES-256，ECB 模式，PKCS#7 填充（Java 侧 PKCS5Padding 等价）</li>
 *   <li>AES 密钥：32 字节（32 字符随机 hex 字符串的 UTF-8 字节，按原始字节使用，不经过 KDF）</li>
 *   <li>非对称算法：RSA-2048，PKCS#1 v1.5（仅用于加密 AES 密钥）</li>
 *   <li>密钥传输：header["encrypt-key"] = RSA_pub(Base64(aesKey_utf8_bytes))</li>
 *   <li>请求体：body = Base64(AES_encrypt(JSON, aesKey))</li>
 * </ul>
 */
public final class EncryptUtils {

    private EncryptUtils() {
    }

    /**
     * 用 RSA 私钥解密请求头中的 AES 密钥
     *
     * @param encryptKeyHeader 请求头 {@code encrypt-key} 的值（RSA 公钥加密的 Base64(aesKey)）
     * @param privateKeyBase64 服务端 RSA 私钥（Base64 PKCS#8）
     * @return AES 密钥原始字节（32 字节）
     */
    public static byte[] decryptAesKey(String encryptKeyHeader, String privateKeyBase64) {
        RSA rsa = SecureUtil.rsa(privateKeyBase64, null);
        String aesKeyBase64 = rsa.decryptStr(encryptKeyHeader, KeyType.PrivateKey);
        return Base64.getDecoder().decode(aesKeyBase64);
    }

    /**
     * 用 AES 密钥解密请求体
     *
     * @param bodyBase64 请求体（Base64(AES_encrypt(JSON))）
     * @param aesKey     AES 密钥原始字节
     * @return 明文 JSON 字符串
     */
    public static String decryptBody(String bodyBase64, byte[] aesKey) {
        AES aes = SecureUtil.aes(aesKey);
        byte[] ciphertext = Base64.getDecoder().decode(bodyBase64);
        byte[] plaintext = aes.decrypt(ciphertext);
        return new String(plaintext, StandardCharsets.UTF_8);
    }
}
