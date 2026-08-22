package org.fellow99.tpl.appapi.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.fellow99.tpl.appapi.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    @Select("SELECT * FROM sys_user WHERE user_name = #{userName} AND del_flag = '0' ORDER BY user_id LIMIT 1")
    SysUser selectByUserName(String userName);

    @Select("SELECT * FROM sys_user WHERE phone_number = #{phoneNumber} AND del_flag = '0' ORDER BY user_id LIMIT 1")
    SysUser selectByPhoneNumber(String phoneNumber);
}
