package com.hourlystore.dto;

import jakarta.validation.constraints.Min;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UpdateFlashSaleRequest {
    private Long couponId;
    private Long productId;
    private String title;
    @Min(0)
    private Integer stock;
    private LocalDateTime beginTime;
    private LocalDateTime endTime;
    private Integer status;
}
