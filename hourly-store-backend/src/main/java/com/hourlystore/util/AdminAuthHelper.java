package com.hourlystore.util;

import com.hourlystore.common.BizException;
import com.hourlystore.entity.User;
import com.hourlystore.mapper.UserMapper;
import com.hourlystore.service.SessionService;
import lombok.experimental.UtilityClass;

@UtilityClass
public class AdminAuthHelper {

    public static Long requireAdmin(SessionService sessionService, UserMapper userMapper, String authorizationHeader) {
        Long userId = AuthHelper.requireUserId(sessionService, authorizationHeader);
        User user = userMapper.selectById(userId);
        if (user == null || user.getRole() == null || user.getRole() != User.ROLE_ADMIN) {
            throw new BizException(403, "Admin access required");
        }
        return userId;
    }
}
