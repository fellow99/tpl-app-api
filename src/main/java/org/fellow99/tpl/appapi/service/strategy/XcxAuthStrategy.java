package org.fellow99.tpl.appapi.service.strategy;

import org.fellow99.tpl.appapi.model.MessageKey;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.fellow99.tpl.appapi.config.properties.SocialLoginConfigProperties;
import org.fellow99.tpl.appapi.entity.SysSocial;
import org.fellow99.tpl.appapi.entity.SysUser;
import org.fellow99.tpl.appapi.entity.UserProfile;
import org.fellow99.tpl.appapi.mapper.SysUserMapper;
import org.fellow99.tpl.appapi.mapper.UserProfileMapper;
import org.fellow99.tpl.appapi.model.dto.SocialLoginResult;
import org.fellow99.tpl.appapi.model.dto.XcxLoginBody;
import org.fellow99.tpl.appapi.service.SysSocialService;
import org.fellow99.tpl.appapi.util.WechatAuthUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.zhyd.oauth.model.AuthResponse;
import me.zhyd.oauth.model.AuthToken;
import me.zhyd.oauth.model.AuthUser;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 小程序认证策略（wechat_xcx，一键登录）。
 *
 * <p>bean 名 = {@code "xcx"} + {@link IAuthStrategy#BASE_NAME}。</p>
 */
@Slf4j
@Service("xcx" + IAuthStrategy.BASE_NAME)
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "wechat", name = "enabled", havingValue = "true", matchIfMissing = true)
public class XcxAuthStrategy implements IAuthStrategy {

    private final WechatAuthUtils wechatAuthUtils;

    private final SysSocialService sysSocialService;

    private final SysUserMapper sysUserMapper;

    private final UserProfileMapper userProfileMapper;

    private final StringRedisTemplate stringRedisTemplate;

    private static final String XCX_SOURCE = "wechat_xcx";

    private static final String XCX_ACCESS_TOKEN_KEY = "tpl:wechat_xcx_access_token:";

    @Override
    public SocialLoginResult login(String body) {
        XcxLoginBody loginBody = JSONUtil.toBean(body, XcxLoginBody.class);
        String xcxCode = loginBody.getXcxCode();
        if (StrUtil.isBlank(xcxCode)) {
            throw new IllegalArgumentException(MessageKey.WECHAT_XCX_CODE_REQUIRED);
        }

        // jscode2session → { openid, session_key, unionid? }
        AuthResponse<AuthUser> response = wechatAuthUtils.loginAuth(XCX_SOURCE, xcxCode, null);
        if (!response.ok()) {
            throw new IllegalArgumentException(response.getMsg());
        }
        AuthToken token = response.getData().getToken();
        String openid = token.getOpenId();
        String unionId = token.getUnionId();

        // authId = "wechat_xcx" + openid
        String authId = XCX_SOURCE + openid;
        SysSocial social = sysSocialService.selectByAuthId(authId);
        if (social != null) {
            SysUser user = loadUser(social.getUserId());
            StpUtil.login(user.getUserId());
            log.info("用户小程序登录成功: userId={}", user.getUserId());
            return SocialLoginResult.bound(StpUtil.getTokenValue());
        }

        // 未绑定 → 需要手机号（前端 getPhoneNumber 获取 phoneCode 后再次请求）
        String phoneCode = loginBody.getPhoneCode();
        if (StrUtil.isBlank(phoneCode)) {
            return SocialLoginResult.needPhone(openid, unionId);
        }

        String phone = getPhoneNumber(phoneCode);
        Long userId = bindOrCreateUser(phone);
        insertSocial(userId, authId, openid, unionId);
        StpUtil.login(userId);
        log.info("用户小程序登录并绑定成功: userId={}", userId);
        return SocialLoginResult.bound(StpUtil.getTokenValue());
    }

    private SysUser loadUser(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new IllegalArgumentException(MessageKey.USER_NOT_FOUND);
        }
        if ("1".equals(user.getStatus())) {
            throw new IllegalArgumentException(MessageKey.USER_DISABLED);
        }
        return user;
    }

    /**
     * 按手机号查用户，存在则复用 userId，不存在则建号（无密码）。
     */
    private Long bindOrCreateUser(String phone) {
        SysUser user = sysUserMapper.selectByPhoneNumber(phone);
        if (user != null) {
            return user.getUserId();
        }
        SysUser sysUser = new SysUser();
        sysUser.setUserName(phone);
        sysUser.setNickName(phone);
        sysUser.setPhoneNumber(phone);
        sysUser.setPassword("");
        sysUser.setStatus("0");
        sysUser.setDelFlag("0");
        sysUser.setCreateTime(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        sysUserMapper.insert(sysUser);

        UserProfile profile = new UserProfile();
        profile.setUserId(sysUser.getUserId());
        profile.setCreateTime(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        userProfileMapper.insert(profile);
        return sysUser.getUserId();
    }

    private void insertSocial(Long userId, String authId, String openid, String unionId) {
        SysSocial s = new SysSocial();
        s.setUserId(userId);
        s.setAuthId(authId);
        s.setSource(XCX_SOURCE);
        s.setOpenId(openid);
        // 小程序无第三方 username，占位为 openid（满足 user_name 非空约束）
        s.setUserName(openid);
        s.setNickName("");
        // session_key 为数据解密凭证，不落库明文（本轮未使用）；access_token 列非空，置空串
        s.setAccessToken("");
        s.setUnionId(unionId);
        s.setCreateTime(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        sysSocialService.insert(s);
    }

    /**
     * 调用微信 getuserphonenumber 获取手机号（需先取 access_token）。
     */
    private String getPhoneNumber(String phoneCode) {
        String accessToken = getAccessToken();
        String url = "https://api.weixin.qq.com/wxa/business/getuserphonenumber?access_token=" + accessToken;
        JSONObject body = new JSONObject();
        body.set("code", phoneCode);
        String respBody = HttpRequest.post(url)
            .contentType("application/json")
            .body(body.toString())
            .execute()
            .body();
        JSONObject json = JSONUtil.parseObj(respBody);
        if (json.getInt("errcode", -1) != 0) {
            log.warn("获取微信手机号失败: {}", json.getStr("errmsg"));
            throw new IllegalArgumentException(MessageKey.WECHAT_GET_PHONE_FAILED);
        }
        JSONObject phoneInfo = json.getJSONObject("phone_info");
        String phone = phoneInfo == null ? null : phoneInfo.getStr("phoneNumber");
        if (StrUtil.isBlank(phone)) {
            throw new IllegalArgumentException(MessageKey.WECHAT_GET_PHONE_FAILED);
        }
        return phone;
    }

    /**
     * 获取小程序全局 access_token（client_credential）。
     */
    private String getAccessToken() {
        SocialLoginConfigProperties cfg = wechatAuthUtils.configOf(XCX_SOURCE);
        if (cfg == null) {
            throw new IllegalArgumentException(MessageKey.WECHAT_XCX_CONFIG_MISSING);
        }
        String cacheKey = XCX_ACCESS_TOKEN_KEY + cfg.getClientId();
        String cached = stringRedisTemplate.opsForValue().get(cacheKey);
        if (StrUtil.isNotBlank(cached)) {
            return cached;
        }
        String url = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid="
            + cfg.getClientId() + "&secret=" + cfg.getClientSecret();
        String respBody = HttpRequest.get(url).execute().body();
        JSONObject json = JSONUtil.parseObj(respBody);
        String accessToken = json.getStr("access_token");
        if (StrUtil.isBlank(accessToken)) {
            log.warn("获取微信access_token失败: {}", json.getStr("errmsg"));
            throw new IllegalArgumentException(MessageKey.WECHAT_ACCESS_TOKEN_FAILED);
        }
        // 缓存 7000 秒（微信 access_token 有效期 7200 秒），避免每次换手机号都携带 secret 请求
        stringRedisTemplate.opsForValue().set(cacheKey, accessToken, Duration.ofSeconds(7000));
        return accessToken;
    }
}
