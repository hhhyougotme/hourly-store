package com.hourlystore.dto;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class PlaceProductOrderRequest {
    @Min(1)
    private Integer quantity = 1;
}
