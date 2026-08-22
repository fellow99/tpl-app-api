package org.fellow99.tpl.appapi.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * {@link SmsProperties} 配置默认值单元测试（对应 test-cases.md TC-07）。
 */
class SmsPropertiesTest {

    @Test
    void defaultValuesShouldApply() {
        SmsProperties properties = new SmsProperties();
        assertEquals("https://push.spug.cc", properties.getBaseUrl());
        assertEquals(5000, properties.getConnectTimeout());
        assertEquals(10000, properties.getReadTimeout());
    }

    @Test
    void templateCodeShouldHaveNoDefault() {
        SmsProperties properties = new SmsProperties();
        assertNull(properties.getTemplateCode());
    }
}
