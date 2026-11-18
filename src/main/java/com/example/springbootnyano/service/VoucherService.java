package com.example.springbootnyano.service;

import com.example.springbootnyano.dto.request.voucher.VoucherFilterRequest;
import com.example.springbootnyano.dto.request.voucher.VoucherRequest;
import com.example.springbootnyano.dto.response.voucher.VoucherBulkResult;
import com.example.springbootnyano.entity.voucher.VoucherEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface VoucherService {
    String effectiveStatusOf(VoucherEntity v);
    List<VoucherEntity> list();
    List<VoucherEntity> activeList();
    VoucherBulkResult bulkCreate(List<VoucherRequest> rows);
    int softDeleteByFilter(VoucherFilterRequest filter);
    VoucherEntity create(VoucherRequest request);
    VoucherEntity update(Long id, VoucherRequest request);
    VoucherEntity toggle(Long id);
    int softDelete(List<Long> ids);
    int restore(List<Long> ids);
    Map<String, Object> validate(String code, BigDecimal subtotal);
}