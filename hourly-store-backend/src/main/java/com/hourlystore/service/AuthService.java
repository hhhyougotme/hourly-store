package com.hourlystore.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hourlystore.common.BizException;
import com.hourlystore.config.AuthProperties;
import com.hourlystore.dto.LoginRequest;
import com.hourlystore.dto.LoginResponse;
import com.hourlystore.dto.RegisterRequest;
import com.hourlystore.dto.SendVerificationCodeRequest;
import com.hourlystore.dto.VerificationCodeResponse;
import com.hourlystore.entity.User;
import com.hourlystore.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final SessionService sessionService;
    private final VerificationCodeService verificationCodeService;
    private final AuthProperties authProperties;

    public VerificationCodeResponse sendRegisterVerificationCode(SendVerificationCodeRequest req) {
        String demo = verificationCodeService.sendRegisterCode(req.getPhone(), req.getEmail());
        return VerificationCodeResponse.sent(authProperties.getVerificationCodeTtlMinutes(), demo);
    }

    @Transactional
    public LoginResponse register(RegisterRequest req) {
        boolean hasPhone = req.getPhone() != null && !req.getPhone().isBlank();
        boolean hasEmail = req.getEmail() != null && !req.getEmail().isBlank();
        if (hasPhone == hasEmail) {
            throw new BizException("Provide exactly one of phone or email");
        }

        verificationCodeService.verifyRegisterCode(req.getPhone(), req.getEmail(), req.getVerificationCode());

        if (hasPhone) {
            Long c = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getPhone, req.getPhone().trim()));
            if (c != null && c > 0) {
                throw new BizException("Phone already registered");
            }
        } else {
            String email = req.getEmail().trim().toLowerCase();
            Long c = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getEmail, email));
            if (c != null && c > 0) {
                throw new BizException("Email already registered");
            }
        }

        User u = new User();
        if (hasPhone) {
            u.setPhone(req.getPhone().trim());
        } else {
            u.setEmail(req.getEmail().trim().toLowerCase());
        }
        u.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        String display = req.getNickname();
        if (display == null || display.isBlank()) {
            display = hasPhone ? req.getPhone().trim() : req.getEmail().trim();
        }
        u.setNickname(display);
        u.setStatus(1);
        u.setRole(User.ROLE_USER);
        userMapper.insert(u);

        return buildLoginResponse(u);
    }

    public LoginResponse login(LoginRequest req) {
        User u = findByAccount(req.getAccount().trim());
        if (u == null || !passwordEncoder.matches(req.getPassword(), u.getPasswordHash())) {
            throw new BizException("Invalid account or password");
        }
        if (u.getStatus() != null && u.getStatus() != 1) {
            throw new BizException("Account disabled");
        }
        return buildLoginResponse(u);
    }

    private LoginResponse buildLoginResponse(User u) {
        String token = sessionService.createSession(u.getId());
        LoginResponse r = new LoginResponse();
        r.setToken(token);
        r.setUserId(u.getId());
        r.setNickname(u.getNickname());
        r.setRole(u.getRole() != null ? u.getRole() : User.ROLE_USER);
        return r;
    }

    private User findByAccount(String account) {
        if (account.contains("@")) {
            return userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getEmail, account.toLowerCase()));
        }
        return userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getPhone, account));
    }
}
