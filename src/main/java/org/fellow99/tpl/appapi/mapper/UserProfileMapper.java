package org.fellow99.tpl.appapi.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.fellow99.tpl.appapi.entity.UserProfile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserProfileMapper extends BaseMapper<UserProfile> {

    @Select("SELECT * FROM tpl_user_profile WHERE user_id = #{userId}")
    UserProfile selectByUserId(Long userId);
}
