package org.fellow99.tpl.appapi.controller;

import org.fellow99.tpl.appapi.model.R;
import org.fellow99.tpl.appapi.model.dto.ChangePasswordRequest;
import org.fellow99.tpl.appapi.model.dto.UpdateProfileRequest;
import org.fellow99.tpl.appapi.model.vo.UserInfoVO;
import org.fellow99.tpl.appapi.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户信息接口（「我的」栏目）：获取信息、修改信息、修改密码。
 */
@Slf4j
@RestController
@RequestMapping("/system/user")
@RequiredArgsConstructor
public class SystemUserController {

    private final UserService userService;

    @GetMapping("/getInfo")
    public R<UserInfoVO> getInfo() {
        try {
            return R.ok(userService.getInfo());
        } catch (IllegalArgumentException e) {
            return R.fail(e.getMessage());
        }
    }

    @GetMapping("/isEmptyPassword")
    public R<Boolean> isEmptyPassword() {
        try {
            return R.ok(userService.isEmptyPassword());
        } catch (IllegalArgumentException e) {
            return R.fail(e.getMessage());
        }
    }

    @PutMapping("/profile")
    public R<Void> updateProfile(@RequestBody UpdateProfileRequest req) {
        try {
            userService.updateProfile(req);
            return R.ok();
        } catch (IllegalArgumentException e) {
            return R.fail(e.getMessage());
        }
    }

    @PutMapping("/password")
    public R<Void> changePassword(@RequestBody ChangePasswordRequest req) {
        try {
            userService.changePassword(req);
            return R.ok();
        } catch (IllegalArgumentException e) {
            return R.fail(e.getMessage());
        }
    }
}
