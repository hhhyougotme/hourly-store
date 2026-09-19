package com.hourlystore.service;

import com.hourlystore.common.BizException;
import com.hourlystore.config.AuthProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class VerificationCodeService {

    public static final String PURPOSE_REGISTER = "REGISTER";

    private final StringRedisTemplate stringRedisTemplate;
    private final AuthProperties authProperties;

    public String sendRegisterCode(String phone, String email) {
        String identifier = normalizeIdentifier(phone, email);
        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000));
        String key = authProperties.getVerificationPrefix() + PURPOSE_REGISTER + ":" + identifier;
        int minutes = Math.max(1, authProperties.getVerificationCodeTtlMinutes());
        stringRedisTemplate.opsForValue().set(key, code, Duration.ofMinutes(minutes));
        return authProperties.isExposeVerificationCodeInResponse() ? code : null;
    }

    public void verifyRegisterCode(String phone, String email, String submittedCode) {
        if (submittedCode == null || submittedCode.isBlank()) {
            throw new BizException("Verification code is required");
        }
        String identifier = normalizeIdentifier(phone, email);
        String key = authProperties.getVerificationPrefix() + PURPOSE_REGISTER + ":" + identifier;
        String expected = stringRedisTemplate.opsForValue().get(key);
        if (expected == null) {
            throw new BizException("Verification code expired or not sent");
        }
        if (!expected.equals(submittedCode.trim())) {
            throw new BizException("Invalid verification code");
        }
        stringRedisTemplate.delete(key);
    }

    private static String normalizeIdentifier(String phone, String email) {
        boolean hasPhone = phone != null && !phone.isBlank();
        boolean hasEmail = email != null && !email.isBlank();
        if (hasPhone == hasEmail) {
            throw new BizException("Provide exactly one of phone or email for verification");
        }
        return hasPhone ? "phone:" + phone.trim() : "email:" + email.trim().toLowerCase();
    }
}
