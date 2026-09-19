package com.hourlystore.controller;

import com.hourlystore.common.ApiResult;
import com.hourlystore.dto.CreateFlashSaleRequest;
import com.hourlystore.dto.UpdateFlashSaleRequest;
import com.hourlystore.entity.FlashSaleEvent;
import com.hourlystore.mapper.UserMapper;
import com.hourlystore.service.FlashSaleAdminService;
import com.hourlystore.service.SessionService;
import com.hourlystore.util.AdminAuthHelper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/flash-sales")
@RequiredArgsConstructor
public class AdminFlashSaleController {

    private final FlashSaleAdminService flashSaleAdminService;
    private final SessionService sessionService;
    private final UserMapper userMapper;

    @PostMapping
    public ApiResult<FlashSaleEvent> create(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody CreateFlashSaleRequest req) {
        AdminAuthHelper.requireAdmin(sessionService, userMapper, authorization);
        return ApiResult.ok(flashSaleAdminService.create(req));
    }

    @PutMapping("/{id}")
    public ApiResult<FlashSaleEvent> update(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id,
            @Valid @RequestBody UpdateFlashSaleRequest req) {
        AdminAuthHelper.requireAdmin(sessionService, userMapper, authorization);
        return ApiResult.ok(flashSaleAdminService.update(id, req));
    }
}
