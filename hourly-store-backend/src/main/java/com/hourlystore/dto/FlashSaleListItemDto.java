package com.hourlystore.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Active flash-sale row plus optional linked product (for display). */
@Data
public class FlashSaleListItemDto {
    private Long id;
    private Long couponId;
    private Long productId;
    private String title;
    private Integer stock;
    private LocalDateTime beginTime;
    private LocalDateTime endTime;
    private Integer status;
    private String productName;
    private BigDecimal productPrice;
}
