package org.fellow99.tpl.appapi.util;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.asymmetric.KeyType;
import cn.hutool.crypto.asymmetric.RSA;
import cn.hutool.crypto.symmetric.AES;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * AES+RSA 混合加密正确性验证（Java 侧往返：模拟前端加密 → 后端解密）。
 */
class EncryptUtilsTest {

    private static final String PRIVATE_KEY =
            "MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQCf9m4itnyAcPGXAmcdKerg+cup+H0Wyq27Kwz+Yl9MIfvSys+IBe2niDkYRP8Cukcbyd7XOOY5kK1UOlMqErgNPeVFw6cpyM/DBbSFL5IYQBdrAgWTBn8ZvrsjDfcfcmVzksd3TNkHMVT88D3APKhvimRXwRTHtXJw7/m4jBKfEKdIV6Dg5ACElCy0wYBcxgVEXVK173mkAKKVEdE5c4F2HbFJslCAcVGWsbsuuRcpeGs5CcXwRAHwYV4teEA112hEMn7P3/0lDsoAAZ19v+zFrGk0EdAW1fLpWr/J1DkR1RYTOtaXMCN+NeDKBmIlyzo0XKJ/lf2hQEf0MD5jXnHDAgMBAAECggEABOpDXRMitvxIjWuxF01CJdKI33OFy/78LwvSijh5B7Or1utwvj6eKoS22/Rvqiuf4yrBfTAG42PQYH+q9REpLZvfNnFITlzf2kpEpI5YB1or6LQalr6J+AZgrHA8W9MPo76HHQxCKq8+wTlUUNqsSZfE2Imo+xOBJjOPsUTOx6tksQ5gR+rkhZpI1X7lZyMjDmoSuci3aOJG6XC2yD1MWTL70uPPb1d2kWfZLbBo+0b1Q+M6jQj6H+qoNbDEr74l36n/n82ViK61ZSgG8Jna73YXYaFK1TE2YIBQLdntlgSUZkhdTTMWeq+VqmreDNpI3JbMTfR8fxCi4vikR4AP/QKBgQDZ4oMXDgsn2C/r6d3XxyEHV6NNGosx4uHDxepPJLIJPH++uOAtvxWLDB05BINH96/JSSGX66eocTuhLazN4xEWIJsasvORa3gfqyFZgyctQOr5mMmzQE+GvYEp8mXAFh0IaOTTdFJuljd1u2ISGi2X/yLxDeOppfTeYCyGz0WwvwKBgQC78gBMUB3QVw6XVoYl1FPaS06v5EAWesf0ff7ZMNHO+dkzRMRr0gEGXnwGkhyb+wVAxIArN0ZQL/7ptgcw6imbYacsvR7RxlAXr4csDGgPhPbOp+bZuwne9GJJzibWRg+Br9xY6wkj8+273/e2bQYQIgtIqm3iDCSxrJiUgi17/QKBgEnHmfxElSJ44kFQf/6BP33s/y8svdW2rhxM+Hq0QlU+V0ON9tuGyRS8lWipTosgJDZUvTtkYPF45mg9vzY4naCQifUQg8nKSnLuz+wvrSR1xxud0S8Rp9xIb2R04F+bHVmrq1CvIvqvgzgqq5rVonrFul9GBMF9oN1sU6eSqFahAoGAL7VSlgyQRN6UeZ4hF2E6l+Md6lBKZGpub4W8N23JFH0fwX/nTGdWk0NQwGo+kOm7f6Fh15aEZr6IOtNWJ/iI70Rup1Cqjh2FHz4TVf8gOzBJZlQAqaxs6QeoG4wVF7dZIFZ/3KJtA49b/aDwxIIQU/AQZTW+Bu+GMQPbB3e0DBECgYAkuvYc4P4dDded863dasy2UMv0LoygE0oPmaiCM9d4/fcXjGBgIWd1VOPeE3ccfYBKWj+h3SONLBYWWMtJiU8B27haimq8eIpUc6hp2FlNN0Y6yhdG1Xxy8g2noJ1wWwCgVNkkh5RRmiVHlH4Kqbe+drg3psfI0EA9cZwQEgOIMg==";

    private static final String PUBLIC_KEY =
            "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAn/ZuIrZ8gHDxlwJnHSnq4PnLqfh9FsqtuysM/mJfTCH70srPiAXtp4g5GET/ArpHG8ne1zjmOZCtVDpTKhK4DT3lRcOnKcjPwwW0hS+SGEAXawIFkwZ/Gb67Iw33H3Jlc5LHd0zZBzFU/PA9wDyob4pkV8EUx7VycO/5uIwSnxCnSFeg4OQAhJQstMGAXMYFRF1Ste95pACilRHROXOBdh2xSbJQgHFRlrG7LrkXKXhrOQnF8EQB8GFeLXhANddoRDJ+z9/9JQ7KAAGdfb/sxaxpNBHQFtXy6Vq/ydQ5EdUWEzrWlzAjfjXgygZiJcs6NFyif5X9oUBH9DA+Y15xwwIDAQAB";

    @Test
    void javaRoundtrip() {
        String hex = "0123456789abcdef0123456789abcdef";
        byte[] aesKey = hex.getBytes(StandardCharsets.UTF_8);
        String plaintext = "{\"phoneNumber\":\"13800138000\",\"password\":\"secret123\"}";

        // 模拟前端：AES-ECB 加密 + RSA-PKCS1 加密 AES key
        AES aes = SecureUtil.aes(aesKey);
        byte[] cipher = aes.encrypt(plaintext.getBytes(StandardCharsets.UTF_8));
        String body = Base64.getEncoder().encodeToString(cipher);

        String aesKeyBase64 = Base64.getEncoder().encodeToString(aesKey);
        RSA rsa = SecureUtil.rsa(null, PUBLIC_KEY);
        byte[] encKeyBytes = rsa.encrypt(aesKeyBase64.getBytes(StandardCharsets.UTF_8), KeyType.PublicKey);
        String encryptKey = Base64.getEncoder().encodeToString(encKeyBytes);

        // 后端解密
        byte[] decAesKey = EncryptUtils.decryptAesKey(encryptKey, PRIVATE_KEY);
        String decrypted = EncryptUtils.decryptBody(body, decAesKey);

        assertEquals(plaintext, decrypted);
    }
}
