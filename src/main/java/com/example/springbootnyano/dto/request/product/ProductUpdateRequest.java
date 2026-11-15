package com.example.springbootnyano.dto.request.product;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ProductUpdateRequest {
    private String productName;
    private BigDecimal productPrice;
}