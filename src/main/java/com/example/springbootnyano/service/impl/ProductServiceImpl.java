package com.example.springbootnyano.service.impl;

import com.example.springbootnyano.dto.request.product.*;
import com.example.springbootnyano.dto.response.product.BulkCreateResult;
import com.example.springbootnyano.entity.ProductEntity;
import com.example.springbootnyano.entity.productvariant.ProductVariantEntity;
import com.example.springbootnyano.repository.OptionCatalogRepository;
import com.example.springbootnyano.repository.ProductRepository;
import com.example.springbootnyano.repository.ProductVariantRepository;
import com.example.springbootnyano.service.ProductService;
import com.example.springbootnyano.util.CatalogFormats;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.example.springbootnyano.entity.voucher.VoucherEntity;
import com.example.springbootnyano.repository.VoucherRepository;
import com.example.springbootnyano.service.VoucherService;
import java.math.RoundingMode;

import java.math.BigDecimal;
import java.util.*;

@Service
public class ProductServiceImpl implements ProductService {

    @Autowired private ProductRepository productRepository;
    @Autowired private ProductVariantRepository productVariantRepository;
    @Autowired private OptionCatalogRepository catalogRepository;

    private static final Set<String> AXES = Set.of("COLOR", "TENSION", "STRING", "GRIP");
    private static final Map<String, String> AXIS_LABEL = Map.of(
            "COLOR", "Màu sắc", "TENSION", "Độ căng dây", "STRING", "Loại dây", "GRIP", "Trọng lượng / cỡ cán");

    @Override
    @Transactional
    public BulkCreateResult bulkCreateVariantsFromRows(Long productId, BulkRowsRequest request) {
        if (!productRepository.existsById(productId))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy sản phẩm " + productId);
        List<VariantRequest> rows = request.getRows();
        if (rows == null || rows.isEmpty())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bản xem trước chưa có biến thể nào");
        if (rows.size() > 40)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tối đa 40 biến thể mỗi lần thêm hàng loạt");
        Set<String> keys = new HashSet<>();
        for (ProductVariantEntity x : productVariantRepository.findByProductId(productId)) keys.add(specKey(x));
        List<String> skipped = new ArrayList<>();
        int created = 0;
        for (VariantRequest r : rows) {
            ProductVariantEntity v = toVariant(productId, r);
            String key = specKey(v);
            if (!keys.add(key)) { skipped.add(v.getColor() + " / " + v.getTension() + " / " + v.getWeightGrip()); continue; }
            productVariantRepository.save(v);
            created++;
        }
        return new BulkCreateResult(created, skipped.size(), skipped);
    }

    @Override
    @Transactional
    public ProductEntity createProduct(ProductCreateRequest request) {
        checkDuplicateName(request.getProductName(), null);
        ProductEntity product = new ProductEntity();
        product.setProductName(request.getProductName().trim());
        product.setProductPrice(request.getProductPrice());
        product.setBrand(normalizeBrand(request.getBrand()));
        product = productRepository.save(product);
        if (request.getVariants() != null) {
            for (VariantRequest vr : request.getVariants()) {
                ProductVariantEntity v = toVariant(product.getId(), vr);
                if (isDuplicateVariant(product.getId(), v, null)) throw dupVariant();
                productVariantRepository.save(v);
            }
        }
        return product;
    }

    @Override
    @Transactional
    public Map<String, Object> deleteVariantsByFilter(Long productId, VariantFilterRequest f) {
        if (!productRepository.existsById(productId))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy sản phẩm " + productId);
        String color = blankToNull(f.getColor()), string = blankToNull(f.getStringType());
        String tension = blankToNull(f.getTension()), grip = blankToNull(f.getWeightGrip());
        if (color == null && string == null && tension == null && grip == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chọn ít nhất một tiêu chí để xóa");
        if (color != null) color = normalizeCatalog("COLOR", color, "Màu sắc");
        if (string != null) string = normalizeCatalog("STRING", string, "Loại dây");
        if (tension != null) tension = normalizeCatalog("TENSION", tension, "Độ căng dây");
        if (grip != null) grip = normalizeCatalog("GRIP", grip, "Trọng lượng / cỡ cán");
        final String c = color, s = string, t = tension, g = grip;
        List<ProductVariantEntity> matched = productVariantRepository.findByProductId(productId).stream()
                .filter(v -> (c == null || fold(v.getColor()).equals(fold(c)))
                        && (s == null || fold(v.getStringType()).equals(fold(s)))
                        && (t == null || fold(v.getTension()).equals(fold(t)))
                        && (g == null || fold(v.getWeightGrip()).equals(fold(g))))
                .collect(java.util.stream.Collectors.toList());
        productVariantRepository.deleteAll(matched);
        return Map.of("deleted", matched.size());
    }

    private String blankToNull(String s) { return s == null || s.isBlank() ? null : s; }
    @Override
    @Transactional
    public ProductEntity updateProduct(Long id, ProductUpdateRequest request) {
        ProductEntity product = productRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy sản phẩm " + id));
        checkDuplicateName(request.getProductName(), id);
        product.setProductName(request.getProductName().trim());
        product.setProductPrice(request.getProductPrice());
        product.setBrand(normalizeBrand(request.getBrand()));
        return productRepository.save(product);
    }

    @Override
    @Transactional
    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy sản phẩm " + id);
        productVariantRepository.deleteAll(productVariantRepository.findByProductId(id));
        productRepository.deleteById(id);
    }

    @Override
    @Transactional
    public ProductVariantEntity addVariant(Long productId, VariantRequest request) {
        if (!productRepository.existsById(productId))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy sản phẩm " + productId);
        ProductVariantEntity v = toVariant(productId, request);
        if (isDuplicateVariant(productId, v, null)) throw dupVariant();
        return productVariantRepository.save(v);
    }

    @Override
    @Transactional
    public ProductVariantEntity updateVariant(Long variantId, VariantRequest request) {
        ProductVariantEntity v = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy biến thể " + variantId));
        applyVariant(v, request);
        if (isDuplicateVariant(v.getProductId(), v, variantId)) throw dupVariant();
        return productVariantRepository.save(v);
    }

    @Override
    @Transactional
    public void deleteVariant(Long variantId) {
        if (!productVariantRepository.existsById(variantId))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy biến thể " + variantId);
        productVariantRepository.deleteById(variantId);
    }

    @Override
    @Transactional
    public BulkCreateResult bulkCreateVariants(Long productId, BulkVariantRequest request) {
        if (!productRepository.existsById(productId))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy sản phẩm " + productId);
        String axis = request.getAxis() == null ? "" : request.getAxis().trim().toUpperCase();
        if (!AXES.contains(axis))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Trục nhân bản không hợp lệ");
        List<String> values = request.getValues();
        if (values == null || values.isEmpty())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chưa chọn giá trị nào để nhân bản");
        if (values.size() > 10)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tối đa 10 biến thể mỗi lần thêm hàng loạt");
        VariantRequest base = request.getBase();
        if (base == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thiếu thông tin biến thể gốc");

        String color   = axis.equals("COLOR")   ? null : normalizeCatalog("COLOR", base.getColor(), "Màu sắc");
        String string  = axis.equals("STRING")  ? null : normalizeCatalog("STRING", base.getStringType(), "Loại dây");
        String tension = axis.equals("TENSION") ? null : normalizeCatalog("TENSION", base.getTension(), "Độ căng dây");
        String grip    = axis.equals("GRIP")    ? null : normalizeCatalog("GRIP", base.getWeightGrip(), "Trọng lượng / cỡ cán");

        Set<String> keys = new HashSet<>();
        for (ProductVariantEntity x : productVariantRepository.findByProductId(productId)) keys.add(specKey(x));
        List<String> skipped = new ArrayList<>();
        int created = 0;
        for (String raw : values) {
            String val = normalizeCatalog(axis, raw, AXIS_LABEL.get(axis));
            ProductVariantEntity v = new ProductVariantEntity();
            v.setProductId(productId);
            v.setColor(axis.equals("COLOR") ? val : color);
            v.setStringType(axis.equals("STRING") ? val : string);
            v.setTension(axis.equals("TENSION") ? val : tension);
            v.setWeightGrip(axis.equals("GRIP") ? val : grip);
            applyCommon(v, base);
            if (!keys.add(specKey(v))) { skipped.add(val); continue; }
            productVariantRepository.save(v);
            created++;
        }
        return new BulkCreateResult(created, skipped.size(), skipped);
    }

    @Override
    @Transactional
    public Map<String, Object> bulkDeleteProducts(List<Long> ids) {
        if (ids == null || ids.isEmpty())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chưa chọn sản phẩm nào");
        if (ids.size() > 100)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tối đa 100 mục mỗi lần xóa");
        int dp = 0, dv = 0;
        for (Long id : ids) {
            if (!productRepository.existsById(id)) continue;
            List<ProductVariantEntity> vs = productVariantRepository.findByProductId(id);
            productVariantRepository.deleteAll(vs);
            dv += vs.size();
            productRepository.deleteById(id);
            dp++;
        }
        if (dp == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy sản phẩm nào để xóa");
        return Map.of("deletedProducts", dp, "deletedVariants", dv);
    }

    @Override
    @Transactional
    public Map<String, Object> bulkDeleteVariants(List<Long> ids) {
        if (ids == null || ids.isEmpty())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chưa chọn biến thể nào");
        if (ids.size() > 100)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tối đa 100 mục mỗi lần xóa");
        int d = 0;
        for (Long id : ids) {
            if (!productVariantRepository.existsById(id)) continue;
            productVariantRepository.deleteById(id);
            d++;
        }
        if (d == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy biến thể nào để xóa");
        return Map.of("deleted", d);
    }

    private ProductVariantEntity toVariant(Long productId, VariantRequest r) {
        ProductVariantEntity v = new ProductVariantEntity();
        v.setProductId(productId);
        applyVariant(v, r);
        return v;
    }

    // imports: com.example.springbootnyano.entity.voucher.VoucherEntity,
    //          com.example.springbootnyano.repository.VoucherRepository,
    //          com.example.springbootnyano.service.VoucherService, java.math.RoundingMode
    @Autowired private VoucherRepository voucherRepository;
    @Autowired private VoucherService voucherService;

    private void applyVariant(ProductVariantEntity v, VariantRequest r) {
        v.setColor(normalizeCatalog("COLOR", r.getColor(), "Màu sắc"));
        v.setStringType(normalizeCatalog("STRING", r.getStringType(), "Loại dây"));
        v.setTension(normalizeCatalog("TENSION", r.getTension(), "Độ căng dây"));
        v.setWeightGrip(normalizeCatalog("GRIP", r.getWeightGrip(), "Trọng lượng / cỡ cán"));
        if (r.getPrice() == null || r.getPrice().signum() <= 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Giá bán phải lớn hơn 0");
        v.setPrice(r.getPrice());
        boolean incoming = "INCOMING".equalsIgnoreCase(r.getStatus());
        int stock = incoming ? 0 : (r.getStockQuantity() == null ? 0 : r.getStockQuantity());
        if (stock < 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tồn kho không được âm");
        if (stock > 1000000) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tồn kho tối đa 1.000.000");
        v.setStockQuantity(stock);
        // XOR: hoặc giảm trực tiếp, hoặc gắn voucher ITEM — không bao giờ cả hai
        if (r.getVoucherId() != null) {
            VoucherEntity vo = voucherRepository.findById(r.getVoucherId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Voucher gắn không tồn tại"));
            if (Boolean.TRUE.equals(vo.getDeleted()))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Voucher gắn đã bị xóa");
            if (!"ITEM".equals(vo.getScope()))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chỉ gắn được voucher phạm vi ITEM (gắn sản phẩm)");
            v.setVoucherId(vo.getId());
            v.setDiscountType(null);
            v.setDiscountValue(null);
        } else {
            v.setVoucherId(null);
            v.setDiscountType(r.getDiscountType());
            v.setDiscountValue(r.getDiscountValue());
            validateDiscount(v);
        }
        v.setStatus(incoming ? "INCOMING" : (stock > 0 ? "AVAILABLE" : "OUT_OF_STOCK"));
    }

    @Override
    public BigDecimal effectivePriceOf(ProductVariantEntity v) {
        BigDecimal price = v.getPrice();
        BigDecimal d = null;
        if (v.getVoucherId() != null) {
            VoucherEntity vo = voucherRepository.findById(v.getVoucherId()).orElse(null);
            if (vo != null && !Boolean.TRUE.equals(vo.getDeleted()) && "ITEM".equals(vo.getScope())
                    && "ACTIVE".equals(voucherService.effectiveStatusOf(vo))) {
                d = "PERCENT".equals(vo.getDiscountType())
                        ? price.multiply(vo.getDiscountValue()).divide(BigDecimal.valueOf(100), 0, RoundingMode.FLOOR)
                        : vo.getDiscountValue();
                if (vo.getMaxDiscountAmount() != null && d.compareTo(vo.getMaxDiscountAmount()) > 0) d = vo.getMaxDiscountAmount();
            }
        } else if (v.getDiscountType() != null && v.getDiscountValue() != null) {
            d = "PERCENT".equals(v.getDiscountType())
                    ? price.multiply(v.getDiscountValue()).divide(BigDecimal.valueOf(100), 0, RoundingMode.FLOOR)
                    : v.getDiscountValue();
        }
        if (d == null) return price;
        if (d.compareTo(price) > 0) d = price;
        return price.subtract(d);
    }

    @Override
    public List<ProductVariantEntity> listVariantsDecorated(Long productId) {
        List<ProductVariantEntity> list = productVariantRepository.findByProductId(productId);
        for (ProductVariantEntity v : list) {
            v.setEffectivePrice(effectivePriceOf(v));
            if (v.getVoucherId() != null)
                voucherRepository.findById(v.getVoucherId()).ifPresent(vo -> v.setVoucherCode(vo.getCode()));
        }
        return list;
    }

    @Override
    public List<ProductEntity> listProductsDecorated() {
        List<ProductEntity> list = productRepository.findAll();
        for (ProductEntity p : list) {
            List<ProductVariantEntity> vs = listVariantsDecorated(p.getId());
            BigDecimal min = vs.stream().map(ProductVariantEntity::getEffectivePrice)
                    .filter(Objects::nonNull).min(BigDecimal::compareTo).orElse(p.getProductPrice());
            p.setMinPrice(min);
        }
        return list;
    }

    private void applyCommon(ProductVariantEntity v, VariantRequest r) {
        if (r.getPrice() == null || r.getPrice().signum() <= 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Giá bán phải lớn hơn 0");
        v.setPrice(r.getPrice());
        boolean incoming = "INCOMING".equalsIgnoreCase(r.getStatus());
        int stock = incoming ? 0 : (r.getStockQuantity() == null ? 0 : r.getStockQuantity());
        if (stock < 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tồn kho không được âm");
        if (stock > 1000000) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tồn kho tối đa 1.000.000");
        v.setStockQuantity(stock);
        v.setDiscountType(r.getDiscountType());
        v.setDiscountValue(r.getDiscountValue());
        validateDiscount(v);
        v.setStatus(incoming ? "INCOMING" : (stock > 0 ? "AVAILABLE" : "OUT_OF_STOCK"));
    }

    private String normalizeCatalog(String category, String value, String label) {
        String cleaned = CatalogFormats.clean(value);
        if (cleaned.isEmpty())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, label + " không được để trống");
        String v = CatalogFormats.canonical(category, cleaned);
        if (!catalogRepository.existsByCategoryAndOptionValueIgnoreCase(category, v))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    label + " \"" + v + "\" chưa có trong danh mục. Hãy thêm vào danh mục trước khi dùng.");
        return v;
    }

    private String normalizeBrand(String brand) {
        if (brand == null || brand.isBlank()) return null;
        return normalizeCatalog("BRAND", brand, "Thương hiệu");
    }

    private void checkDuplicateName(String name, Long excludeId) {
        String n = CatalogFormats.clean(name);
        if (n.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên sản phẩm không được để trống");
        String folded = CatalogFormats.stripAccents(n);
        boolean dup = productRepository.findAll().stream()
                .anyMatch(p -> !p.getId().equals(excludeId) && p.getProductName() != null
                        && CatalogFormats.stripAccents(p.getProductName().trim()).equalsIgnoreCase(folded));
        if (dup) throw new ResponseStatusException(HttpStatus.CONFLICT, "Tên sản phẩm \"" + n + "\" đã tồn tại (kể cả viết không dấu)");
    }

    private boolean isDuplicateVariant(Long productId, ProductVariantEntity v, Long excludeId) {
        String key = specKey(v);
        return productVariantRepository.findByProductId(productId).stream()
                .filter(x -> !x.getId().equals(excludeId))
                .anyMatch(x -> specKey(x).equals(key));
    }

    private String specKey(ProductVariantEntity v) {
        return String.join("|", fold(v.getColor()), fold(v.getStringType()), fold(v.getTension()), fold(v.getWeightGrip()));
    }

    private String fold(String s) {
        return CatalogFormats.stripAccents(s == null ? "" : s.trim()).toLowerCase();
    }

    private ResponseStatusException dupVariant() {
        return new ResponseStatusException(HttpStatus.CONFLICT, "Biến thể trùng thông số (màu + dây + độ căng + cỡ cán) đã tồn tại");
    }

    private void validateDiscount(ProductVariantEntity v) {
        if (v.getDiscountType() == null || v.getDiscountType().isBlank()) {
            v.setDiscountType(null); v.setDiscountValue(null); return;
        }
        String type = v.getDiscountType().trim().toUpperCase();
        if (!type.equals("PERCENT") && !type.equals("FIXED"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "discountType phải là PERCENT hoặc FIXED");
        if (v.getDiscountValue() == null || v.getDiscountValue().signum() < 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "discountValue phải là số >= 0");
        if (type.equals("PERCENT") && v.getDiscountValue().compareTo(BigDecimal.valueOf(100)) > 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Giảm theo PERCENT không được vượt quá 100");
        if (type.equals("FIXED") && v.getPrice() != null && v.getDiscountValue().compareTo(v.getPrice()) > 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Giảm cố định không được vượt quá giá bán");
        v.setDiscountType(type);
    }
}