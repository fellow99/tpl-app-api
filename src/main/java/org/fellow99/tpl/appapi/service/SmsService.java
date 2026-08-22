package org.fellow99.tpl.appapi.service;

import org.fellow99.tpl.appapi.model.MessageKey;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.fellow99.tpl.appapi.config.SmsProperties;
import org.fellow99.tpl.appapi.model.dto.SmsSendResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 短信服务功能封装类
 *
 * <p>对接 Spug Push 短信平台，实现短信验证码发送、缓存去重与消费校验能力，供注册、登录、找回密码等业务模块调用。</p>
 *
 * <p>用法：</p>
 * <pre>
 * smsService.sendCode(phone);                     // 发送验证码（一个手机号一个验证码，10 分钟有效）
 * boolean ok = smsService.verifyCode(phone, code); // 校验并消费验证码
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SmsService {

    /** 手机号校验：11 位数字，以 1 开头 */
    private static final String PHONE_PATTERN = "^1\\d{10}$";

    /** 验证码校验：4-6 位数字或字母 */
    private static final String CODE_PATTERN = "^[0-9a-zA-Z]{4,6}$";

    /** 短信接口路径模板 */
    private static final String SMS_PATH = "/sms/%s";

    /** 验证码 Redis Key 模板：{}=手机号 */
    private static final String SMS_CODE_REDIS_KEY = "sms_code:{}";

    /** 验证码缓存有效期（分钟） */
    private static final int SMS_CODE_TTL_MINUTES = 10;

    /** 验证码长度（纯数字） */
    private static final int CODE_LENGTH = 6;

    private final SmsProperties smsProperties;
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 发送短信验证码（含缓存去重机制）
     *
     * <ul>
     *   <li>若该手机号已缓存验证码且未过期，则不重新生成、不重复发送，直接返回成功；</li>
     *   <li>否则生成新验证码、缓存 10 分钟，并触发短信平台发送。</li>
     * </ul>
     *
     * @param phone 接收手机号（11 位）
     * @return 发送结果；命中缓存时 requestId 为空
     * @throws IllegalArgumentException 手机号格式非法
     * @throws IllegalStateException    短信发送失败（网络异常/超时）
     */
    public SmsSendResult sendCode(String phone) {
        validatePhone(phone);

        String key = smsCodeKey(phone);
        String cached = stringRedisTemplate.opsForValue().get(key);
        if (StrUtil.isNotBlank(cached)) {
            log.debug("验证码未过期，跳过重复发送: phone={}", maskPhone(phone));
            SmsSendResult result = new SmsSendResult();
            result.setCode(SmsSendResult.SUCCESS_CODE);
            result.setMsg(MessageKey.SMS_NOT_EXPIRED);
            return result;
        }

        String code = generateCode();
        stringRedisTemplate.opsForValue().set(key, code, SMS_CODE_TTL_MINUTES, TimeUnit.MINUTES);
        log.debug("已缓存验证码: phone={}, ttl={}分钟", maskPhone(phone), SMS_CODE_TTL_MINUTES);

        SmsSendResult result;
        try {
            result = sendVerificationCode(phone, code);
        } catch (RuntimeException e) {
            // 发送失败（网络异常/超时）时回滚缓存，避免残留用户未收到的验证码
            stringRedisTemplate.delete(key);
            throw e;
        }
        if (!result.isSuccess()) {
            // 发送失败（平台业务错误）时回滚缓存
            stringRedisTemplate.delete(key);
            log.warn("短信发送失败，已回滚验证码缓存: phone={}", maskPhone(phone));
        }
        return result;
    }

    /**
     * 校验并消费验证码
     *
     * <p>若输入验证码与缓存的手机号-验证码一致，则清空缓存并返回 true；否则返回 false。</p>
     *
     * @param phone 手机号
     * @param code  输入验证码
     * @return true=验证成功（已消费），false=验证失败
     */
    public boolean verifyCode(String phone, String code) {
        if (StrUtil.isBlank(phone) || StrUtil.isBlank(code)) {
            return false;
        }

        String key = smsCodeKey(phone);
        String cached = stringRedisTemplate.opsForValue().get(key);
        if (cached == null || !cached.equals(code)) {
            return false;
        }

        stringRedisTemplate.delete(key);
        log.debug("验证码消费成功: phone={}", maskPhone(phone));
        return true;
    }

    /**
     * 发送短信验证码（底层，直接发送指定验证码，不做缓存）
     *
     * @param phone 接收手机号（11 位）
     * @param code  验证码（4-6 位数字或字母）
     * @return 发送结果（含 request_id，{@link SmsSendResult#isSuccess()} 判定是否成功）
     * @throws IllegalArgumentException 手机号或验证码格式非法
     * @throws IllegalStateException    短信发送失败（网络异常/超时）
     */
    public SmsSendResult sendVerificationCode(String phone, String code) {
        validatePhone(phone);
        validateCode(code);

        String url = buildUrl();
        String body = buildBody(phone, code);

        try {
            String responseBody = doPost(url, body);
            log.debug("短信发送响应: body={}", responseBody);
            return parseResult(responseBody);
        } catch (Exception e) {
            log.error("短信发送失败: phone={}, url={}", maskPhone(phone), url, e);
            throw new IllegalStateException(MessageKey.SMS_SEND_FAILED, e);
        }
    }

    /**
     * 执行 HTTP POST 并返回响应体（包级可见，便于测试注入）
     */
    String doPost(String url, String body) {
        HttpResponse response = HttpRequest.post(url)
                .contentType("application/json")
                .body(body)
                .setConnectionTimeout(smsProperties.getConnectTimeout())
                .timeout(smsProperties.getReadTimeout())
                .execute();
        return response.body();
    }

    private void validatePhone(String phone) {
        if (StrUtil.isBlank(phone) || !phone.matches(PHONE_PATTERN)) {
            throw new IllegalArgumentException(MessageKey.PHONE_INVALID);
        }
    }

    private void validateCode(String code) {
        if (StrUtil.isBlank(code) || !code.matches(CODE_PATTERN)) {
            throw new IllegalArgumentException(MessageKey.CAPTCHA_INVALID_FORMAT);
        }
    }

    private String generateCode() {
        return RandomUtil.randomNumbers(CODE_LENGTH);
    }

    private String smsCodeKey(String phone) {
        return SMS_CODE_REDIS_KEY.replace("{}", phone);
    }

    /**
     * 手机号脱敏：保留前 3 位与后 4 位，中间以 **** 代替
     */
    private static String maskPhone(String phone) {
        if (StrUtil.isBlank(phone) || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    private String buildUrl() {
        return smsProperties.getBaseUrl() + String.format(SMS_PATH, smsProperties.getTemplateCode());
    }

    private String buildBody(String phone, String code) {
        JSONObject json = new JSONObject();
        json.set("to", phone);
        json.set("code", code);
        // 带有效时长的模板必填 number（有效分钟数），与验证码缓存 TTL 一致
        json.set("number", String.valueOf(SMS_CODE_TTL_MINUTES));
        return json.toString();
    }

    private SmsSendResult parseResult(String responseBody) {
        JSONObject json = JSONUtil.parseObj(responseBody);
        SmsSendResult result = new SmsSendResult();
        result.setCode(json.getInt("code", -1));
        result.setMsg(json.getStr("msg"));
        result.setRequestId(json.getStr("request_id"));
        return result;
    }
}
