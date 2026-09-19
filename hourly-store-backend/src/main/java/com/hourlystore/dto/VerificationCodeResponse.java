package com.hourlystore.dto;

import lombok.Data;

@Data
public class VerificationCodeResponse {
    private String message;
    /** Present only when hourlystore.auth.expose-verification-code-in-response=true (demo). */
    private String demoCode;
    private int expiresInMinutes;

    public static VerificationCodeResponse sent(int minutes, String demoCode) {
        VerificationCodeResponse r = new VerificationCodeResponse();
        r.setMessage("Verification code sent");
        r.setExpiresInMinutes(minutes);
        r.setDemoCode(demoCode);
        return r;
    }
}
