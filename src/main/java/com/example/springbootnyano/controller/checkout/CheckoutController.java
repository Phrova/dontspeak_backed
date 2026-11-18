package com.example.springbootnyano.controller.checkout;

import com.example.springbootnyano.dto.request.checkout.CheckoutPreviewRequest;
import com.example.springbootnyano.entity.productvariant.ProductVariantEntity;
import com.example.springbootnyano.entity.voucher.VoucherEntity;
import com.example.springbootnyano.repository.ProductRepository;
import com.example.springbootnyano.repository.ProductVariantRepository;
import com.example.springbootnyano.repository.VoucherRepository;
import com.example.springbootnyano.service.ProductService;
import com.example.springbootnyano.service.VoucherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/checkout")
public class CheckoutController {

    @Autowired private ProductVariantRepository variantRepo;
    @Autowired private ProductRepository productRepo;
    @Autowired private VoucherRepository voucherRepo;
    @Autowired private ProductService productService;
    @Autowired private VoucherService voucherService;

    @PostMapping("/preview")
    public Map<String, Object> preview(@RequestBody CheckoutPreviewRequest req) {
        if (req.getItems() == null || req.getItems().isEmpty())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Giỏ hàng trống");
        int totalQty = 0;
        List<Map<String, Object>> lines = new ArrayList<>();
        Set<String> attachedCodes = new HashSet<>();
        BigDecimal original = BigDecimal.ZERO, subtotal = BigDecimal.ZERO;
        for (CheckoutPreviewRequest.Line l : req.getItems()) {
            if (l.getVariantId() == null || l.getQuantity() == null || l.getQuantity() < 1)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số lượng không hợp lệ");
            totalQty += l.getQuantity();
            if (totalQty > 20)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Giỏ hàng chỉ được chứa tối đa 20 sản phẩm");
            ProductVariantEntity v = variantRepo.findById(l.getVariantId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sản phẩm trong giỏ không còn tồn tại"));
            if (!productRepo.existsById(v.getProductId()))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sản phẩm trong giỏ đã bị gỡ khỏi cửa hàng");
            if ("INCOMING".equals(v.getStatus()))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Có sản phẩm sắp về hàng, chưa mở bán");
            if (v.getStockQuantity() < l.getQuantity())
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Chỉ còn " + v.getStockQuantity() + " sản phẩm cho phân loại " + v.getColor() + " / " + v.getTension());
            BigDecimal unit = v.getPrice();
            BigDecimal eff = productService.effectivePriceOf(v);
            if (v.getVoucherId() != null)
                voucherRepo.findById(v.getVoucherId()).ifPresent(vo -> attachedCodes.add(vo.getCode().toUpperCase()));
            BigDecimal lineTotal = eff.multiply(BigDecimal.valueOf(l.getQuantity()));
            original = original.add(unit.multiply(BigDecimal.valueOf(l.getQuantity())));
            subtotal = subtotal.add(lineTotal);
            Map<String, Object> line = new LinkedHashMap<>();
            line.put("variantId", v.getId());
            line.put("productId", v.getProductId());
            line.put("variantInfo", v.getColor() + " - " + v.getStringType() + " - " + v.getTension() + " - " + v.getWeightGrip());
            line.put("unitPrice", unit);
            line.put("effectiveUnitPrice", eff);
            line.put("quantity", l.getQuantity());
            line.put("lineTotal", lineTotal);
            line.put("stock", v.getStockQuantity());
            lines.add(line);
        }
        BigDecimal orderDiscount = BigDecimal.ZERO;
        String usedCode = null;
        if (req.getVoucherCode() != null && !req.getVoucherCode().isBlank()) {
            String code = req.getVoucherCode().trim().toUpperCase();
            VoucherEntity vo = voucherRepo.findByDeletedFalse().stream()
                    .filter(x -> x.getCode().equalsIgnoreCase(code)).findFirst()
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã giảm giá không hợp lệ hoặc không áp dụng được"));
            if ("ITEM".equals(vo.getScope()))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã này áp dụng tự động cho sản phẩm, không nhập tay được");
            if (attachedCodes.contains(code))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã này đã áp dụng cho sản phẩm trong giỏ, không dùng lại được");
            String eff = voucherService.effectiveStatusOf(vo);
            if (!"ACTIVE".equals(eff))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã giảm giá không hợp lệ hoặc không áp dụng được");
            if (subtotal.compareTo(vo.getMinOrderValue()) < 0)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Đơn chưa đạt tối thiểu " + vo.getMinOrderValue().toBigInteger() + " ₫ để dùng mã này");
            orderDiscount = "PERCENT".equals(vo.getDiscountType())
                    ? subtotal.multiply(vo.getDiscountValue()).divide(BigDecimal.valueOf(100), 0, RoundingMode.FLOOR)
                    : vo.getDiscountValue();
            if (vo.getMaxDiscountAmount() != null && orderDiscount.compareTo(vo.getMaxDiscountAmount()) > 0)
                orderDiscount = vo.getMaxDiscountAmount();
            if (orderDiscount.compareTo(subtotal) > 0) orderDiscount = subtotal;
            usedCode = vo.getCode();
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("lines", lines);
        out.put("originalSubtotal", original);
        out.put("itemDiscount", original.subtract(subtotal));
        out.put("subtotal", subtotal);
        out.put("orderDiscount", orderDiscount);
        out.put("voucherCode", usedCode);
        out.put("total", subtotal.subtract(orderDiscount));
        return out;
    }
}