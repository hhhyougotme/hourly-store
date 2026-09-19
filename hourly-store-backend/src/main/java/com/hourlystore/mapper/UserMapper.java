package com.hourlystore.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hourlystore.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
