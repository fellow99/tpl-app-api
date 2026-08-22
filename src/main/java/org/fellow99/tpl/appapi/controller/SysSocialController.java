package org.fellow99.tpl.appapi.controller;

import org.fellow99.tpl.appapi.model.MessageKey;
import cn.dev33.satoken.stp.StpUtil;
import org.fellow99.tpl.appapi.entity.SysSocial;
import org.fellow99.tpl.appapi.model.R;
import org.fellow99.tpl.appapi.model.vo.SysSocialVO;
import org.fellow99.tpl.appapi.service.SysSocialService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 社会化关系接口：当前登录用户的第三方绑定列表。
 */
@Slf4j
@RestController
@RequestMapping("/system/social")
@RequiredArgsConstructor
public class SysSocialController {

    private final SysSocialService sysSocialService;

    @Value("${wechat.enabled:true}")
    private boolean wechatEnabled;

    /**
     * 查询当前登录用户的社会化账号绑定列表。
     */
    @GetMapping("/list")
    public R<List<SysSocialVO>> list() {
        if (!wechatEnabled) {
            return R.fail(MessageKey.WECHAT_NOT_ENABLED);
        }
        List<SysSocialVO> vos = sysSocialService.selectByUserId(StpUtil.getLoginIdAsLong())
            .stream()
            .map(SysSocialController::toVO)
            .toList();
        return R.ok(vos);
    }

    /**
     * 实体转 VO：剔除 accessToken / refreshToken 等敏感字段，避免泄露 session_key。
     */
    private static SysSocialVO toVO(SysSocial s) {
        SysSocialVO vo = new SysSocialVO();
        vo.setId(s.getId());
        vo.setUserId(s.getUserId());
        vo.setAuthId(s.getAuthId());
        vo.setSource(s.getSource());
        vo.setOpenId(s.getOpenId());
        vo.setNickName(s.getNickName());
        vo.setAvatar(s.getAvatar());
        vo.setUnionId(s.getUnionId());
        vo.setCreateTime(s.getCreateTime());
        return vo;
    }
}
