package com.example.springbootnyano.dto.request.product;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class ProductCreateRequest {
    private String productName;
    private BigDecimal productPrice;
    private List<VariantRequest> variants;
}