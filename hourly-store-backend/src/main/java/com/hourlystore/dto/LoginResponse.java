package com.hourlystore.dto;

import lombok.Data;

@Data
public class LoginResponse {
    private String token;
    private Long userId;
    private String nickname;
    private int role;
}
