package com.example.springbootnyano.dto.request.voucher;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class VoucherFilterRequest {
    private String discountType;
    private String effectiveStatus;
    private LocalDateTime endBefore;
}