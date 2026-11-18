package com.example.springbootnyano.dto.request.voucher;

import lombok.Data;
import java.util.List;

@Data
public class VoucherIdsRequest {
    private List<Long> ids;
}