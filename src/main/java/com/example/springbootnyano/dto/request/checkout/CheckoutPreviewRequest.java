package com.example.springbootnyano.dto.request.checkout;

import lombok.Data;
import java.util.List;

@Data
public class CheckoutPreviewRequest {
    private List<Line> items;
    private String voucherCode;
    @Data
    public static class Line {
        private Long variantId;
        private Integer quantity;
    }
}