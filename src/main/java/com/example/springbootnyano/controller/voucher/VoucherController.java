package com.example.springbootnyano.controller.voucher;

import com.example.springbootnyano.dto.request.voucher.VoucherValidateRequest;
import com.example.springbootnyano.entity.voucher.VoucherEntity;
import com.example.springbootnyano.service.VoucherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/voucher")
public class VoucherController {

    @Autowired private VoucherService voucherService;

    @GetMapping("/getAll")
    public List<VoucherEntity> getAll() { return voucherService.list(); }

    @GetMapping("/active")
    public List<VoucherEntity> active() { return voucherService.activeList(); }

    @PostMapping("/validate")
    public Map<String, Object> validate(@RequestBody VoucherValidateRequest body) {
        return voucherService.validate(body.getCode(), body.getSubtotal());
    }
}