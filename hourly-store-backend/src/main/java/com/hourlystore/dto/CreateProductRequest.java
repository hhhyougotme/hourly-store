package com.hourlystore.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateProductRequest {
    @NotNull
    private Long merchantId;

    @NotBlank
    @Size(max = 255)
    private String name;

    @Size(max = 1024)
    private String description;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal price;

    @NotNull
    @Min(0)
    private Integer stock;

    @Size(max = 512)
    private String imageUrl;
}
