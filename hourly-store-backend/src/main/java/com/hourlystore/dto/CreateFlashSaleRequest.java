package com.hourlystore.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreateFlashSaleRequest {
    @NotNull
    private Long couponId;
    private Long productId;
    private String title;
    @NotNull
    @Min(0)
    private Integer stock;
    @NotNull
    private LocalDateTime beginTime;
    @NotNull
    private LocalDateTime endTime;
    private Integer status = 1;
}
