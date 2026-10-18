package com.loja.orderservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponse {
    private String sku;
    private String name;
    private BigDecimal price;
    private BigDecimal weightKg;
    private Long stock;
    private Boolean discontinued;
}
