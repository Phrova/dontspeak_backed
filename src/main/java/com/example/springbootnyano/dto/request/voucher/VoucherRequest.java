package com.example.springbootnyano.dto.request.voucher;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class VoucherRequest {
    private String code;
    private String name;
    private String description;
    private String discountType;
    private BigDecimal discountValue;
    private BigDecimal minOrderValue;
    private BigDecimal maxDiscountAmount;
    private Integer quantity;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private String status;
    private String scope;
}