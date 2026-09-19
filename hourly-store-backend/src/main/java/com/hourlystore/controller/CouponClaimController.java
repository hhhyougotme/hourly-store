package com.hourlystore.controller;

import com.hourlystore.common.ApiResult;
import com.hourlystore.entity.CouponClaim;
import com.hourlystore.service.CouponClaimService;
import com.hourlystore.service.SessionService;
import com.hourlystore.util.AuthHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CouponClaimController {

    private final CouponClaimService couponClaimService;
    private final SessionService sessionService;

    @PostMapping("/coupons/{couponId}/claim")
    public ApiResult<CouponClaim> claim(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long couponId) {
        Long userId = AuthHelper.requireUserId(sessionService, authorization);
        return ApiResult.ok(couponClaimService.claim(userId, couponId));
    }

    @GetMapping("/me/coupon-claims")
    public ApiResult<List<CouponClaim>> mine(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        Long userId = AuthHelper.requireUserId(sessionService, authorization);
        return ApiResult.ok(couponClaimService.listMine(userId));
    }
}
