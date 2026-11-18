package com.example.springbootnyano.dto.request.product;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class VariantRequest {
    private Long voucherId;
    private String color;
    private String stringType;
    private String tension;
    private String weightGrip;
    private BigDecimal price;
    private Integer stockQuantity;
    private String discountType;
    private BigDecimal discountValue;
    private String status;
}