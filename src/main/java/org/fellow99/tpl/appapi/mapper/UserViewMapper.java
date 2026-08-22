package org.fellow99.tpl.appapi.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.fellow99.tpl.appapi.entity.UserView;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserViewMapper extends BaseMapper<UserView> {

    @Select("SELECT * FROM tpl_user_view WHERE user_name = #{userName} AND del_flag = '0'")
    UserView selectByUserName(String userName);

    @Select("SELECT * FROM tpl_user_view WHERE user_id = #{userId} AND del_flag = '0'")
    UserView selectByUserId(Long userId);
}
