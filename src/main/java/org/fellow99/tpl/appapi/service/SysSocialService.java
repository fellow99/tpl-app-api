package org.fellow99.tpl.appapi.service;

import org.fellow99.tpl.appapi.model.MessageKey;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.fellow99.tpl.appapi.entity.SysSocial;
import org.fellow99.tpl.appapi.mapper.SysSocialMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.zhyd.oauth.model.AuthToken;
import me.zhyd.oauth.model.AuthUser;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 社会化关系服务。
 *
 * <p>对应 RuoYi-Vue-Plus 的 {@code ISysSocialService}/{@code SysSocialServiceImpl}，
 * tpl-app-api 采用具体类 {@code @Service @RequiredArgsConstructor}，不做接口/实现拆分。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysSocialService {

    private final SysSocialMapper socialMapper;

    /**
     * 按 authId（= source + openid）查询绑定记录，不存在返回 null。
     */
    public SysSocial selectByAuthId(String authId) {
        List<SysSocial> list = socialMapper.selectList(
            new LambdaQueryWrapper<SysSocial>()
                .eq(SysSocial::getAuthId, authId)
                .last("limit 1"));
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * 按用户主键查询其绑定的第三方授权列表。
     */
    public List<SysSocial> selectByUserId(Long userId) {
        return socialMapper.selectList(
            new LambdaQueryWrapper<SysSocial>().eq(SysSocial::getUserId, userId));
    }

    public boolean insert(SysSocial social) {
        return socialMapper.insert(social) > 0;
    }

    public boolean updateById(SysSocial social) {
        return socialMapper.updateById(social) > 0;
    }

    public boolean deleteById(Long id) {
        return socialMapper.deleteById(id) > 0;
    }

    /**
     * 绑定第三方账号（对齐 RVP {@code SysLoginService#socialRegister}）。
     *
     * <p>authId = source + uuid；若该 authId 已被绑定则抛异常；否则按「userId + source」
     * 已存在则更新、不存在则新增。</p>
     */
    public void bindSocial(Long userId, String source, AuthUser authUser) {
        String authId = source + authUser.getUuid();
        if (selectByAuthId(authId) != null) {
            throw new IllegalArgumentException(MessageKey.SOCIAL_ALREADY_BOUND);
        }
        List<SysSocial> existing = socialMapper.selectList(
            new LambdaQueryWrapper<SysSocial>()
                .eq(SysSocial::getUserId, userId)
                .eq(SysSocial::getSource, source));
        SysSocial social = buildSocial(userId, source, authUser, authId);
        if (existing.isEmpty()) {
            insert(social);
        } else {
            social.setId(existing.get(0).getId());
            updateById(social);
        }
    }

    private SysSocial buildSocial(Long userId, String source, AuthUser authUser, String authId) {
        SysSocial s = new SysSocial();
        s.setUserId(userId);
        s.setAuthId(authId);
        s.setSource(source);
        s.setOpenId(authUser.getUuid());
        s.setUserName(authUser.getUsername() == null ? authUser.getUuid() : authUser.getUsername());
        s.setNickName(authUser.getNickname());
        s.setEmail(authUser.getEmail());
        s.setAvatar(authUser.getAvatar());
        AuthToken token = authUser.getToken();
        if (token != null) {
            s.setAccessToken(token.getAccessToken());
            s.setExpireIn(token.getExpireIn() == 0 ? null : (long) token.getExpireIn());
            s.setRefreshToken(token.getRefreshToken());
            s.setUnionId(token.getUnionId());
            s.setScope(token.getScope());
            s.setTokenType(token.getTokenType());
        }
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        s.setCreateTime(now);
        s.setUpdateTime(now);
        return s;
    }
}
