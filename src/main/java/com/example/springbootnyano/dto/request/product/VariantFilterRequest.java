package com.example.springbootnyano.dto.request.product;

import lombok.Data;

@Data
public class VariantFilterRequest {
    private String color;
    private String stringType;
    private String tension;
    private String weightGrip;
}