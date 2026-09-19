package com.hourlystore.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PlaceOrderRequest {
    /** Optional; defaults to 1.00 in service if null */
    private BigDecimal amount;
}
