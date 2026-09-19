package com.hourlystore.dto;

import lombok.Data;

@Data
public class SendVerificationCodeRequest {
    private String phone;
    private String email;
}
