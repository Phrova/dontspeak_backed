package com.example.springbootnyano.dto.request.product;

import lombok.Data;
import java.util.List;

@Data
public class BulkVariantRequest {
    private VariantRequest base;
    private String axis;
    private List<String> values;
}