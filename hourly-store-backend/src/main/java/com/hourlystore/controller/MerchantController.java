package com.hourlystore.controller;

import com.hourlystore.common.ApiResult;
import com.hourlystore.dto.PageResult;
import com.hourlystore.entity.Merchant;
import com.hourlystore.service.MerchantService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/merchants")
@RequiredArgsConstructor
public class MerchantController {

    private final MerchantService merchantService;

    @GetMapping
    public ApiResult<PageResult<Merchant>> list(
            @RequestParam(required = false) Long merchantTypeId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResult.ok(merchantService.listPage(merchantTypeId, page, pageSize));
    }

    @GetMapping("/{id}")
    public ApiResult<Merchant> get(@PathVariable Long id) {
        return ApiResult.ok(merchantService.getById(id));
    }
}
