package com.example.springbootnyano.dto.request.voucher;

import lombok.Data;
import java.util.List;

@Data
public class VoucherBulkRowsRequest {
    private List<VoucherRequest> rows;
}