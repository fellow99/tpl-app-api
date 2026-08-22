package org.fellow99.tpl.appapi.service;

import org.fellow99.tpl.appapi.model.MessageKey;
import cn.dev33.satoken.secure.BCrypt;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import org.fellow99.tpl.appapi.entity.SysUser;
import org.fellow99.tpl.appapi.entity.UserProfile;
import org.fellow99.tpl.appapi.mapper.SysUserMapper;
import org.fellow99.tpl.appapi.mapper.UserProfileMapper;
import org.fellow99.tpl.appapi.model.dto.LoginRequest;
import org.fellow99.tpl.appapi.model.dto.LoginResponse;
import org.fellow99.tpl.appapi.model.dto.RegisterRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;

/**
 * 认证服务：注册（手机注册 + 自动登录）、登录（密码 / 短信）、图形验证码、登出。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserMapper sysUserMapper;
    private final UserProfileMapper userProfileMapper;
    private final SmsService smsService;
    private final CaptchaService captchaService;

    /**
     * 手机注册 + 自动登录
     *
     * <ol>
     *   <li>先验证 CAPTCHA；</li>
     *   <li>再调用短信验证码「消费校验」；</li>
     *   <li>判断用户是否存在：不存在则新增并登录，存在则直接密码登录；</li>
     *   <li>登录成功返回 token。</li>
     * </ol>
     */
    @Transactional(rollbackFor = Exception.class)
    public LoginResponse register(RegisterRequest req) {
        // 1. 图形验证码
        captchaService.validate(req.getUuid(), req.getCode());

        // 2. 短信验证码（消费校验）
        if (StrUtil.isBlank(req.getPhoneNumber())) {
            throw new IllegalArgumentException(MessageKey.PHONE_EMPTY);
        }
        if (!smsService.verifyCode(req.getPhoneNumber(), req.getSmsCode())) {
            throw new IllegalArgumentException(MessageKey.SMS_CODE_INCORRECT);
        }

        // 3. 密码
        if (StrUtil.isBlank(req.getPassword())) {
            throw new IllegalArgumentException(MessageKey.PASSWORD_REQUIRED);
        }

        // 4. 判断用户是否存在
        boolean existed = sysUserMapper.selectByUserName(req.getPhoneNumber()) != null;
        if (!existed) {
            try {
                createUser(req.getPhoneNumber(), req.getPassword());
            } catch (Exception e) {
                log.error("创建用户失败: phone={}", maskPhone(req.getPhoneNumber()), e);
                throw new IllegalArgumentException(MessageKey.USER_CREATE_FAILED);
            }
        }

        // 6. 用户/密码登录
        String token = doPasswordLogin(req.getPhoneNumber(), req.getPassword());
        if (token == null) {
            if (existed) {
                throw new IllegalArgumentException(MessageKey.USER_EXISTS_PASSWORD_WRONG);
            }
            throw new IllegalArgumentException(MessageKey.USER_CREATED);
        }

        log.info("用户注册且登录成功: userId={}", StpUtil.getLoginIdAsLong());
        return new LoginResponse(token);
    }

    /**
     * 统一登录：按 grantType 分发密码登录 / 短信登录。
     *
     * <p>social / xcx 由 {@code IAuthStrategy} 在 Controller 层分发，不进入本方法。</p>
     */
    public LoginResponse login(LoginRequest req) {
        String grantType = StrUtil.isBlank(req.getGrantType()) ? "password" : req.getGrantType().trim();
        if ("sms".equals(grantType)) {
            return smsLogin(req);
        }
        return passwordLogin(req);
    }

    private LoginResponse passwordLogin(LoginRequest req) {
        // 图形验证码仅在密码/短信登录路径校验（social/xcx 走策略分发，跳过验证码）
        captchaService.validate(req.getUuid(), req.getCode());
        if (StrUtil.isBlank(req.getPhoneNumber()) || StrUtil.isBlank(req.getPassword())) {
            throw new IllegalArgumentException(MessageKey.PHONE_PASSWORD_REQUIRED);
        }
        String token = doPasswordLogin(req.getPhoneNumber(), req.getPassword());
        if (token == null) {
            throw new IllegalArgumentException(MessageKey.LOGIN_FAILED);
        }
        return new LoginResponse(token);
    }

    private LoginResponse smsLogin(LoginRequest req) {
        // 图形验证码仅在密码/短信登录路径校验
        captchaService.validate(req.getUuid(), req.getCode());
        if (StrUtil.isBlank(req.getPhoneNumber())) {
            throw new IllegalArgumentException(MessageKey.PHONE_EMPTY);
        }
        // 短信验证码（消费校验）
        if (!smsService.verifyCode(req.getPhoneNumber(), req.getSmsCode())) {
            throw new IllegalArgumentException(MessageKey.SMS_CODE_INCORRECT);
        }
        SysUser user = sysUserMapper.selectByUserName(req.getPhoneNumber());
        if (user == null) {
            throw new IllegalArgumentException(MessageKey.USER_NOT_FOUND);
        }
        if ("1".equals(user.getStatus())) {
            throw new IllegalArgumentException(MessageKey.USER_DISABLED);
        }
        StpUtil.login(user.getUserId());
        log.info("用户短信登录成功: userId={}", user.getUserId());
        return new LoginResponse(StpUtil.getTokenValue());
    }

    /**
     * 密码登录核心：成功返回 token，失败返回 null（不抛异常，便于注册/登录复用差异化错误提示）。
     */
    private String doPasswordLogin(String phone, String password) {
        SysUser user = sysUserMapper.selectByUserName(phone);
        if (user == null) {
            return null;
        }
        // 无密码账号（微信一键登录自动建号）不允许密码登录
        if (StrUtil.isBlank(user.getPassword())) {
            throw new IllegalArgumentException(MessageKey.NO_PASSWORD_USE_WECHAT);
        }
        if (!BCrypt.checkpw(password, user.getPassword())) {
            return null;
        }
        if ("1".equals(user.getStatus())) {
            return null;
        }
        StpUtil.login(user.getUserId());
        log.info("用户密码登录成功: userId={}", user.getUserId());
        return StpUtil.getTokenValue();
    }

    private void createUser(String phone, String password) {
        SysUser sysUser = new SysUser();
        sysUser.setUserName(phone);
        sysUser.setNickName(phone);
        sysUser.setPhoneNumber(phone);
        sysUser.setPassword(BCrypt.hashpw(password));
        sysUser.setStatus("0");
        sysUser.setDelFlag("0");
        sysUser.setCreateTime(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        sysUserMapper.insert(sysUser);

        UserProfile profile = new UserProfile();
        profile.setUserId(sysUser.getUserId());
        profile.setCreateTime(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        userProfileMapper.insert(profile);
    }

    public Map<String, Object> generateCaptcha() {
        return captchaService.generateCaptcha();
    }

    private static String maskPhone(String phone) {
        if (StrUtil.isBlank(phone) || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
