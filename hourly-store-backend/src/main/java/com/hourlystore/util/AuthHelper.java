package com.hourlystore.util;

import com.hourlystore.common.BizException;
import com.hourlystore.service.SessionService;
import lombok.experimental.UtilityClass;

@UtilityClass
public class AuthHelper {

    public static Long requireUserId(SessionService sessionService, String authorizationHeader) {
        return sessionService.findUserId(authorizationHeader)
                .orElseThrow(() -> new BizException(401, "Unauthorized"));
    }
}
