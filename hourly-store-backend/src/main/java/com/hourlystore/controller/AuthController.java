package com.hourlystore.controller;

import com.hourlystore.common.ApiResult;
import com.hourlystore.dto.LoginRequest;
import com.hourlystore.dto.LoginResponse;
import com.hourlystore.dto.RegisterRequest;
import com.hourlystore.dto.SendVerificationCodeRequest;
import com.hourlystore.dto.VerificationCodeResponse;
import com.hourlystore.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/verification-code")
    public ApiResult<VerificationCodeResponse> sendVerificationCode(@Valid @RequestBody SendVerificationCodeRequest req) {
        return ApiResult.ok(authService.sendRegisterVerificationCode(req));
    }

    @PostMapping("/register")
    public ApiResult<LoginResponse> register(@Valid @RequestBody RegisterRequest req) {
        return ApiResult.ok(authService.register(req));
    }

    @PostMapping("/login")
    public ApiResult<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        return ApiResult.ok(authService.login(req));
    }
}
