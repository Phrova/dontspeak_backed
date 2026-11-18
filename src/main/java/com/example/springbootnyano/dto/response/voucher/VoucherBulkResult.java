package com.example.springbootnyano.dto.response.voucher;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VoucherBulkResult {
    private int created;
    private int skipped;
    private List<String> skippedValues;
}