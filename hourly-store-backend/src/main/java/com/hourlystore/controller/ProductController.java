package com.hourlystore.controller;

import com.hourlystore.common.ApiResult;
import com.hourlystore.dto.CreateProductRequest;
import com.hourlystore.entity.Product;
import com.hourlystore.service.ProductService;
import com.hourlystore.service.SessionService;
import com.hourlystore.util.AuthHelper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final SessionService sessionService;

    @GetMapping
    public ApiResult<List<Product>> list(@RequestParam Long merchantId) {
        return ApiResult.ok(productService.listOnShelfByMerchant(merchantId));
    }

    @GetMapping("/{id}")
    public ApiResult<Product> get(@PathVariable Long id) {
        return ApiResult.ok(productService.getOnShelfById(id));
    }

    /** Logged-in users may add on-shelf items (demo; no merchant role). */
    @PostMapping
    public ApiResult<Product> create(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody CreateProductRequest body) {
        AuthHelper.requireUserId(sessionService, authorization);
        return ApiResult.ok(productService.create(body));
    }
}
