package com.example.springbootnyano.controller.voucher;

import com.example.springbootnyano.dto.request.voucher.VoucherBulkRowsRequest;
import com.example.springbootnyano.dto.request.voucher.VoucherFilterRequest;
import com.example.springbootnyano.dto.request.voucher.VoucherIdsRequest;
import com.example.springbootnyano.dto.request.voucher.VoucherRequest;
import com.example.springbootnyano.dto.response.voucher.VoucherBulkResult;
import com.example.springbootnyano.entity.voucher.VoucherEntity;
import com.example.springbootnyano.service.VoucherService;
import com.example.springbootnyano.util.AuthGuard;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/admin/voucher")
public class AdminVoucherController {

    @Autowired private AuthGuard guard;
    @Autowired private VoucherService voucherService;

    @PostMapping("/bulk-rows")
    public VoucherBulkResult bulkRows(HttpServletRequest req, @RequestBody VoucherBulkRowsRequest body) {
        guard.requireAdmin(req);
        return voucherService.bulkCreate(body.getRows());
    }

    @PostMapping("/delete-by-filter")
    public Map<String, Object> deleteByFilter(HttpServletRequest req, @RequestBody VoucherFilterRequest body) {
        guard.requireAdmin(req);
        return Map.of("deleted", voucherService.softDeleteByFilter(body));
    }

    @PostMapping("/create")
    public VoucherEntity create(HttpServletRequest req, @RequestBody VoucherRequest body) {
        guard.requireAdmin(req);
        return voucherService.create(body);
    }

    @PutMapping("/{id}")
    public VoucherEntity update(HttpServletRequest req, @PathVariable Long id, @RequestBody VoucherRequest body) {
        guard.requireAdmin(req);
        return voucherService.update(id, body);
    }

    @PostMapping("/{id}/toggle")
    public VoucherEntity toggle(HttpServletRequest req, @PathVariable Long id) {
        guard.requireAdmin(req);
        return voucherService.toggle(id);
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(HttpServletRequest req, @PathVariable Long id) {
        guard.requireAdmin(req);
        return Map.of("deleted", voucherService.softDelete(List.of(id)));
    }

    @PostMapping("/bulk-delete")
    public Map<String, Object> bulkDelete(HttpServletRequest req, @RequestBody VoucherIdsRequest body) {
        guard.requireAdmin(req);
        return Map.of("deleted", voucherService.softDelete(body.getIds()));
    }

    @PostMapping("/restore")
    public Map<String, Object> restore(HttpServletRequest req, @RequestBody VoucherIdsRequest body) {
        guard.requireAdmin(req);
        return Map.of("restored", voucherService.restore(body.getIds()));
    }
}