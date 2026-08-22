package org.fellow99.tpl.appapi.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import org.fellow99.tpl.appapi.model.R;
import org.fellow99.tpl.appapi.model.dto.SmsSendResult;
import org.fellow99.tpl.appapi.service.SmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 短信验证码接口：调用 202-sms-integration 的发送验证码功能。
 */
@Slf4j
@RestController
@RequestMapping("/resource/sms")
@RequiredArgsConstructor
public class SmsController {

    private final SmsService smsService;

    @SaIgnore
    @GetMapping("/code")
    public R<Void> sendCode(@RequestParam String phoneNumber) {
        try {
            SmsSendResult result = smsService.sendCode(phoneNumber);
            if (result.isSuccess()) {
                return R.ok();
            }
            return R.fail(result.getMsg());
        } catch (IllegalArgumentException | IllegalStateException e) {
            return R.fail(e.getMessage());
        }
    }
}
