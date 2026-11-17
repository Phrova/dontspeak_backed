package com.example.springbootnyano.dto.request.product;

import lombok.Data;
import java.util.List;

@Data
public class BulkDeleteRequest {
    private List<Long> ids;
}