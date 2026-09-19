package com.hourlystore.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {
    @Size(max = 32)
    private String phone;

    @Size(max = 128)
    private String email;

    @NotBlank
    @Size(min = 6, max = 64)
    private String password;

    @Size(max = 64)
    private String nickname;

    @NotBlank
    @Size(min = 4, max = 8)
    private String verificationCode;
}
