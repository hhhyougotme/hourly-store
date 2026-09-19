package com.hourlystore.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    /** Phone number or email used at registration. */
    @NotBlank
    private String account;

    @NotBlank
    private String password;
}
