package com.hourlystore.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "hourlystore.auth")
public class AuthProperties {
    private String tokenHeader = "Authorization";
    private String tokenPrefix = "Bearer ";
    private String redisKeyPrefix = "hourlystore:session:";
    private String userSessionPrefix = "hourlystore:user-session:";
    private String verificationPrefix = "hourlystore:verify:";
    private int tokenTtlHours = 168;
    private int verificationCodeTtlMinutes = 5;
    /** Demo only: return code in API response so testers need no SMS gateway. */
    private boolean exposeVerificationCodeInResponse = true;
}
