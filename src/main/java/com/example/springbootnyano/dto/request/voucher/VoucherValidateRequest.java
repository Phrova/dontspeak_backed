package com.example.springbootnyano.dto.request.voucher;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class VoucherValidateRequest {
    private String code;
    private BigDecimal subtotal;
}