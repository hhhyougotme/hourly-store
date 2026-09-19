package com.hourlystore.controller;

import com.hourlystore.common.ApiResult;
import com.hourlystore.dto.FlashSaleListItemDto;
import com.hourlystore.service.FlashSaleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/flash-sales")
@RequiredArgsConstructor
public class FlashSaleController {

    private final FlashSaleService flashSaleService;

    @GetMapping
    public ApiResult<List<FlashSaleListItemDto>> listActive() {
        return ApiResult.ok(flashSaleService.listActive());
    }
}
