package com.example.springbootnyano.dto.request.product;

import lombok.Data;
import java.util.List;

@Data
public class BulkRowsRequest {
    private List<VariantRequest> rows;
}