package com.example.springbootnyano.service.impl;

import com.example.springbootnyano.dto.request.voucher.VoucherFilterRequest;
import com.example.springbootnyano.dto.request.voucher.VoucherRequest;
import com.example.springbootnyano.dto.response.voucher.VoucherBulkResult;
import com.example.springbootnyano.entity.voucher.VoucherEntity;
import com.example.springbootnyano.repository.VoucherRepository;
import com.example.springbootnyano.service.VoucherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class VoucherServiceImpl implements VoucherService {

    @Autowired private VoucherRepository repo;

    private static final Pattern CODE_RE = Pattern.compile("^[A-Z0-9-]{3,20}$");

    @Override
    public String effectiveStatusOf(VoucherEntity v) { return effective(v); }

    @Override
    @Transactional
    public VoucherBulkResult bulkCreate(List<VoucherRequest> rows) {
        if (rows == null || rows.isEmpty())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bản xem trước chưa có mã nào");
        if (rows.size() > 40)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tối đa 40 voucher mỗi lần thêm hàng loạt");
        Set<String> seen = new HashSet<>();
        List<String> skipped = new ArrayList<>();
        int created = 0;
        for (VoucherRequest r : rows) {
            String code = r.getCode() == null ? "" : r.getCode().trim().toUpperCase();
            if (repo.existsByCodeIgnoreCaseAndDeletedFalse(code) || !seen.add(code)) { skipped.add(code); continue; }
            VoucherEntity v = new VoucherEntity();
            apply(v, r, null);
            v.setUsedQuantity(0);
            repo.save(v);
            created++;
        }
        return new VoucherBulkResult(created, skipped.size(), skipped);
    }

    @Override
    @Transactional
    public int softDeleteByFilter(VoucherFilterRequest f) {
        String type = f.getDiscountType() == null || f.getDiscountType().isBlank() ? null : f.getDiscountType().trim().toUpperCase();
        String status = f.getEffectiveStatus() == null || f.getEffectiveStatus().isBlank() ? null : f.getEffectiveStatus().trim().toUpperCase();
        if (type == null && status == null && f.getEndBefore() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chọn ít nhất một tiêu chí để xóa");
        List<VoucherEntity> matched = repo.findByDeletedFalse().stream()
                .filter(v -> (type == null || type.equals(v.getDiscountType()))
                        && (status == null || status.equals(effective(v)))
                        && (f.getEndBefore() == null || v.getEndAt().isBefore(f.getEndBefore())))
                .toList();
        matched.forEach(v -> v.setDeleted(true));
        repo.saveAll(matched);
        return matched.size();
    }

    @Override
    public List<VoucherEntity> list() {
        List<VoucherEntity> all = repo.findByDeletedFalse();
        all.forEach(v -> v.setEffectiveStatus(effective(v)));
        return all;
    }

    @Override
    public List<VoucherEntity> activeList() {
        return list().stream()
                .filter(v -> "ORDER".equals(v.getScope()) && "ACTIVE".equals(v.getEffectiveStatus()))
                .toList();
    }

    @Override
    @Transactional
    public VoucherEntity create(VoucherRequest r) {
        VoucherEntity v = new VoucherEntity();
        apply(v, r, null);
        v.setUsedQuantity(0);
        return withEffective(repo.save(v));
    }

    @Override
    @Transactional
    public VoucherEntity update(Long id, VoucherRequest r) {
        VoucherEntity v = repo.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy voucher " + id));
        apply(v, r, v);
        if (v.getQuantity() < v.getUsedQuantity())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Số lượt mới không được nhỏ hơn lượt đã dùng (" + v.getUsedQuantity() + ")");
        return withEffective(repo.save(v));
    }

    @Override
    @Transactional
    public VoucherEntity toggle(Long id) {
        VoucherEntity v = repo.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy voucher " + id));
        v.setStatus("ACTIVE".equals(v.getStatus()) ? "INACTIVE" : "ACTIVE");
        return withEffective(repo.save(v));
    }

    @Override
    @Transactional
    public int softDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chưa chọn voucher nào");
        if (ids.size() > 100)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tối đa 100 mục mỗi lần xóa");
        int n = 0;
        for (Long id : ids) {
            VoucherEntity v = repo.findByIdAndDeletedFalse(id).orElse(null);
            if (v == null) continue;
            v.setDeleted(true);
            repo.save(v);
            n++;
        }
        if (n == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy voucher nào để xóa");
        return n;
    }

    @Override
    @Transactional
    public int restore(List<Long> ids) {
        if (ids == null || ids.isEmpty())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không có gì để hoàn tác");
        int n = 0;
        for (Long id : ids) {
            VoucherEntity v = repo.findById(id).orElse(null);
            if (v == null || !Boolean.TRUE.equals(v.getDeleted())) continue;
            v.setDeleted(false);
            repo.save(v);
            n++;
        }
        return n;
    }

    @Override
    public Map<String, Object> validate(String code, BigDecimal subtotal) {
        String c = code == null ? "" : code.trim().toUpperCase();
        VoucherEntity v = repo.findByDeletedFalse().stream()
                .filter(x -> x.getCode().equalsIgnoreCase(c)).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Mã giảm giá không tồn tại"));
        if ("ITEM".equals(v.getScope()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã này áp dụng tự động cho sản phẩm, không nhập tay được");
        String eff = effective(v);
        if (!"ACTIVE".equals(eff)) {
            String reason = switch (eff) {
                case "SCHEDULED" -> "Mã chưa đến thời gian áp dụng";
                case "EXPIRED" -> "Mã đã hết hạn";
                case "USED_UP" -> "Mã đã hết lượt sử dụng";
                default -> "Mã đang bị ngừng";
            };
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, reason);
        }
        if (subtotal == null || subtotal.signum() <= 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Giá trị đơn hàng không hợp lệ");
        BigDecimal discount = computeDiscount(v, subtotal);
        return Map.of("voucherId", v.getId(), "code", v.getCode(), "discount", discount,
                "minOrderValue", v.getMinOrderValue(), "effectiveStatus", eff);
    }

    private BigDecimal computeDiscount(VoucherEntity v, BigDecimal subtotal) {
        if (subtotal.compareTo(v.getMinOrderValue()) < 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Đơn hàng chưa đạt giá trị tối thiểu " + v.getMinOrderValue().toBigInteger() + " ₫");
        BigDecimal raw = "PERCENT".equals(v.getDiscountType())
                ? subtotal.multiply(v.getDiscountValue()).divide(BigDecimal.valueOf(100), 0, RoundingMode.FLOOR)
                : v.getDiscountValue();
        if (v.getMaxDiscountAmount() != null && raw.compareTo(v.getMaxDiscountAmount()) > 0)
            raw = v.getMaxDiscountAmount();
        if (raw.compareTo(subtotal) > 0) raw = subtotal;
        return raw;
    }

    private String effective(VoucherEntity v) {
        if ("INACTIVE".equals(v.getStatus())) return "INACTIVE";
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(v.getStartAt())) return "SCHEDULED";
        if (now.isAfter(v.getEndAt())) return "EXPIRED";
        if (v.getUsedQuantity() >= v.getQuantity()) return "USED_UP";
        return "ACTIVE";
    }

    private VoucherEntity withEffective(VoucherEntity v) {
        v.setEffectiveStatus(effective(v));
        return v;
    }

    private void apply(VoucherEntity v, VoucherRequest r, VoucherEntity old) {
        String code = r.getCode() == null ? "" : r.getCode().trim().toUpperCase();
        if (!CODE_RE.matcher(code).matches())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã phải 3–20 ký tự, chỉ chữ hoa, số hoặc gạch ngang (vd: WELCOME50)");
        boolean dup = repo.findByDeletedFalse().stream()
                .anyMatch(x -> x.getCode().equalsIgnoreCase(code) && (old == null || !x.getId().equals(old.getId())));
        if (dup) throw new ResponseStatusException(HttpStatus.CONFLICT, "Mã \"" + code + "\" đã tồn tại");
        String name = r.getName() == null ? "" : r.getName().trim();
        if (name.isEmpty() || name.length() > 100)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên voucher bắt buộc, tối đa 100 ký tự");
        String type = r.getDiscountType() == null ? "" : r.getDiscountType().trim().toUpperCase();
        if (!type.equals("PERCENT") && !type.equals("FIXED"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Kiểu giảm phải là PERCENT hoặc FIXED");
        if (r.getDiscountValue() == null || r.getDiscountValue().signum() <= 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Giá trị giảm phải lớn hơn 0");
        if (type.equals("PERCENT") && r.getDiscountValue().compareTo(BigDecimal.valueOf(100)) > 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Giảm theo % không được vượt quá 100");
        if (r.getMinOrderValue() == null || r.getMinOrderValue().signum() < 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Đơn tối thiểu phải >= 0");
        BigDecimal max = type.equals("FIXED") ? null : r.getMaxDiscountAmount();
        if (max != null && max.signum() < 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mức giảm tối đa phải >= 0");
        if (r.getQuantity() == null || r.getQuantity() < 1 || r.getQuantity() > 100000)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số lượt phải từ 1 đến 100.000");
        if (r.getStartAt() == null || r.getEndAt() == null || !r.getEndAt().isAfter(r.getStartAt()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thời gian kết thúc phải sau thời gian bắt đầu");
        String status = r.getStatus() == null ? "ACTIVE" : r.getStatus().trim().toUpperCase();
        if (!status.equals("ACTIVE") && !status.equals("INACTIVE"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Công tắc chỉ nhận ACTIVE hoặc INACTIVE");
        String scope = r.getScope() == null ? "ORDER" : r.getScope().trim().toUpperCase();
        if (!scope.equals("ORDER") && !scope.equals("ITEM"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phạm vi chỉ nhận ORDER hoặc ITEM");
        v.setScope(scope);
        v.setCode(code);
        v.setName(name);
        v.setDescription(r.getDescription() == null ? null : r.getDescription().trim());
        v.setDiscountType(type);
        v.setDiscountValue(r.getDiscountValue());
        v.setMinOrderValue(r.getMinOrderValue());
        v.setMaxDiscountAmount(max);
        v.setQuantity(r.getQuantity());
        v.setStartAt(r.getStartAt());
        v.setEndAt(r.getEndAt());
        v.setStatus(status);
    }
}