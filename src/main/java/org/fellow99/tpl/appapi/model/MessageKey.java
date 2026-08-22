package org.fellow99.tpl.appapi.model;

/**
 * 面向前端返回的 i18n 消息 key 常量。
 *
 * <p>统一响应 {@link R#getMsg()} 的语义为「i18n key」，前端据此到语料（父工程
 * i18n/{common,app}/*.json）取对应语言文案。禁止在业务代码中散落硬编码中文消息。</p>
 *
 * <p>key 命名规范：信息反馈用 {@code message.*}，错误/校验反馈用 {@code message.error.*}。
 * 与父工程语料 key 严格一致（详见 specs/004-i18n/spec.md 附录三）。</p>
 */
public final class MessageKey {

    private MessageKey() {
    }

    // ---- 信息反馈 ----
    public static final String SUCCESS = "message.success";
    public static final String USER_CREATED = "message.userCreated";
    public static final String SMS_NOT_EXPIRED = "message.smsNotExpired";

    // ---- 通用错误 ----
    public static final String UNAUTHORIZED = "message.error.unauthorized";
    public static final String FORBIDDEN = "message.error.forbidden";
    public static final String BAD_REQUEST = "message.error.badRequest";
    public static final String SERVER_BUSY = "message.error.serverBusy";
    public static final String REQUEST_BODY_EMPTY = "message.error.requestBodyEmpty";
    public static final String DECRYPT_FAILED = "message.error.decryptFailed";

    // ---- 用户 / 认证 ----
    public static final String LOGIN_FAILED = "message.error.loginFailed";
    public static final String USER_NOT_FOUND = "message.error.userNotFound";
    public static final String USER_DISABLED = "message.error.userDisabled";
    public static final String USER_CREATE_FAILED = "message.error.userCreateFailed";
    public static final String USER_EXISTS_PASSWORD_WRONG = "message.error.userExistsPasswordWrong";
    public static final String NO_PASSWORD_USE_WECHAT = "message.error.noPasswordUseWechat";
    public static final String UNSUPPORTED_GRANT_TYPE = "message.error.unsupportedGrantType";

    // ---- 手机号 / 密码 / 校验 ----
    public static final String PHONE_EMPTY = "message.error.phoneEmpty";
    public static final String PHONE_INVALID = "message.error.phoneInvalid";
    public static final String PHONE_PASSWORD_REQUIRED = "message.error.phonePasswordRequired";
    public static final String PASSWORD_REQUIRED = "message.error.passwordRequired";
    public static final String PASSWORD_MISMATCH = "message.error.passwordMismatch";
    public static final String CURRENT_PASSWORD_WRONG = "message.error.currentPasswordWrong";
    public static final String NEW_PASSWORD_REQUIRED = "message.error.newPasswordRequired";
    public static final String NICKNAME_REQUIRED = "message.error.nicknameRequired";

    // ---- 验证码 / 短信 ----
    public static final String CAPTCHA_REQUIRED = "message.error.captchaRequired";
    public static final String CAPTCHA_INCORRECT = "message.error.captchaIncorrect";
    public static final String CAPTCHA_EXPIRED = "message.error.captchaExpired";
    public static final String CAPTCHA_INVALID_FORMAT = "message.error.captchaInvalidFormat";
    public static final String SMS_CODE_INCORRECT = "message.error.smsCodeIncorrect";
    public static final String SMS_SEND_FAILED = "message.error.smsSendFailed";

    // ---- 微信 / 三方登录 ----
    public static final String WECHAT_NOT_ENABLED = "message.error.wechatNotEnabled";
    public static final String WECHAT_LOGIN_FAILED = "message.error.wechatLoginFailed";
    public static final String WECHAT_PLATFORM_UNSUPPORTED = "message.error.wechatPlatformUnsupported";
    public static final String WECHAT_PLATFORM_NOT_CONFIGURED = "message.error.wechatPlatformNotConfigured";
    public static final String WECHAT_UNBOUND_NEED_PHONE = "message.error.wechatUnboundNeedPhone";
    public static final String WECHAT_UNBOUND_USE_PHONE_LOGIN = "message.error.wechatUnboundUsePhoneLogin";
    public static final String WECHAT_XCX_CODE_REQUIRED = "message.error.wechatXcxCodeRequired";
    public static final String WECHAT_GET_PHONE_FAILED = "message.error.wechatGetPhoneFailed";
    public static final String WECHAT_XCX_CONFIG_MISSING = "message.error.wechatXcxConfigMissing";
    public static final String WECHAT_ACCESS_TOKEN_FAILED = "message.error.wechatAccessTokenFailed";
    public static final String WECHAT_UNSUPPORTED_SOURCE = "message.error.wechatUnsupportedSource";
    public static final String WECHAT_INVALID_AUTH_CONFIG = "message.error.wechatInvalidAuthConfig";
    public static final String UNBIND_FAILED = "message.error.unbindFailed";

    // ---- 三方绑定 ----
    public static final String SOCIAL_PARAM_REQUIRED = "message.error.socialParamRequired";
    public static final String SOCIAL_BOUND_USER_NOT_FOUND = "message.error.socialBoundUserNotFound";
    public static final String SOCIAL_ALREADY_BOUND = "message.error.socialAlreadyBound";
}
