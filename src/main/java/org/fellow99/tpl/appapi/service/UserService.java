package org.fellow99.tpl.appapi.service;

import org.fellow99.tpl.appapi.model.MessageKey;
import cn.dev33.satoken.secure.BCrypt;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import org.fellow99.tpl.appapi.entity.SysUser;
import org.fellow99.tpl.appapi.entity.UserProfile;
import org.fellow99.tpl.appapi.entity.UserView;
import org.fellow99.tpl.appapi.mapper.SysUserMapper;
import org.fellow99.tpl.appapi.mapper.UserProfileMapper;
import org.fellow99.tpl.appapi.mapper.UserViewMapper;
import org.fellow99.tpl.appapi.model.dto.ChangePasswordRequest;
import org.fellow99.tpl.appapi.model.dto.UpdateProfileRequest;
import org.fellow99.tpl.appapi.model.vo.UserInfoVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 用户信息服务：获取信息（含 token 续期）、修改信息、修改密码。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserViewMapper userViewMapper;
    private final SysUserMapper sysUserMapper;
    private final UserProfileMapper userProfileMapper;
    private final SmsService smsService;
    private final CaptchaService captchaService;

    @Value("${sa-token.timeout:2592000}")
    private long tokenTimeout;

    /**
     * 获取当前用户信息（后端实现 token 续期）
     */
    public UserInfoVO getInfo() {
        long userId = StpUtil.getLoginIdAsLong();
        // token 续期：保持活跃用户登录态
        StpUtil.renewTimeout(tokenTimeout);

        UserView view = userViewMapper.selectByUserId(userId);
        if (view == null) {
            throw new IllegalArgumentException(MessageKey.USER_NOT_FOUND);
        }
        return toVO(view);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateProfile(UpdateProfileRequest req) {
        captchaService.validate(req.getUuid(), req.getCode());

        long userId = StpUtil.getLoginIdAsLong();
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new IllegalArgumentException(MessageKey.USER_NOT_FOUND);
        }

        if (StrUtil.isBlank(req.getNickName())) {
            throw new IllegalArgumentException(MessageKey.NICKNAME_REQUIRED);
        }

        user.setNickName(req.getNickName());
        user.setEmail(req.getEmail());
        user.setUpdateTime(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        sysUserMapper.updateById(user);

        UserProfile profile = userProfileMapper.selectByUserId(userId);
        if (profile == null) {
            profile = new UserProfile();
            profile.setUserId(userId);
            profile.setBirthDate(req.getBirthDate());
            profile.setCreateTime(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
            profile.setUpdateTime(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
            userProfileMapper.insert(profile);
        } else {
            profile.setBirthDate(req.getBirthDate());
            profile.setUpdateTime(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
            userProfileMapper.updateById(profile);
        }

        log.info("用户信息已更新: userId={}", userId);
    }

    public void changePassword(ChangePasswordRequest req) {
        captchaService.validate(req.getUuid(), req.getCode());

        long userId = StpUtil.getLoginIdAsLong();
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new IllegalArgumentException(MessageKey.USER_NOT_FOUND);
        }

        // 无密码账号（微信一键登录自动建号）免校验当前密码；普通账号校验当前密码
        if (StrUtil.isNotBlank(user.getPassword())
                && (StrUtil.isBlank(req.getOldPassword())
                    || !BCrypt.checkpw(req.getOldPassword(), user.getPassword()))) {
            throw new IllegalArgumentException(MessageKey.CURRENT_PASSWORD_WRONG);
        }

        // 校验新密码一致性
        if (StrUtil.isBlank(req.getNewPassword())) {
            throw new IllegalArgumentException(MessageKey.NEW_PASSWORD_REQUIRED);
        }
        if (!req.getNewPassword().equals(req.getConfirmPassword())) {
            throw new IllegalArgumentException(MessageKey.PASSWORD_MISMATCH);
        }

        // 校验短信验证码（消费）
        if (!smsService.verifyCode(user.getPhoneNumber(), req.getSmsCode())) {
            throw new IllegalArgumentException(MessageKey.SMS_CODE_INCORRECT);
        }

        // 更新密码
        SysUser update = new SysUser();
        update.setUserId(userId);
        update.setPassword(BCrypt.hashpw(req.getNewPassword()));
        update.setUpdateTime(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        sysUserMapper.updateById(update);

        // 修改密码后强制重新登录
        StpUtil.logout();
        log.info("用户密码已修改: userId={}", userId);
    }

    private UserInfoVO toVO(UserView view) {
        UserInfoVO vo = new UserInfoVO();
        vo.setUserId(view.getUserId());
        vo.setUserName(view.getUserName());
        vo.setNickName(view.getNickName());
        vo.setPhoneNumber(view.getPhoneNumber());
        vo.setEmail(view.getEmail());
        vo.setBirthDate(view.getBirthDate());
        return vo;
    }

    /**
     * 判断当前账号是否为无密码账号（微信一键登录自动建号，password 为空）。
     */
    public boolean isEmptyPassword() {
        long userId = StpUtil.getLoginIdAsLong();
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new IllegalArgumentException(MessageKey.USER_NOT_FOUND);
        }
        return StrUtil.isBlank(user.getPassword());
    }
}
