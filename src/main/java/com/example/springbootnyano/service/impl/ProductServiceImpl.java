package com.example.springbootnyano.service.impl;

import com.example.springbootnyano.dto.request.product.ProductCreateRequest;
import com.example.springbootnyano.dto.request.product.ProductUpdateRequest;
import com.example.springbootnyano.dto.request.product.VariantRequest;
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

import java.math.BigDecimal;

@Service
public class ProductServiceImpl implements ProductService {

    @Autowired private ProductRepository productRepository;
    @Autowired private ProductVariantRepository productVariantRepository;
    @Autowired private OptionCatalogRepository catalogRepository;

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

    private ProductVariantEntity toVariant(Long productId, VariantRequest r) {
        ProductVariantEntity v = new ProductVariantEntity();
        v.setProductId(productId);
        applyVariant(v, r);
        return v;
    }

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
        v.setDiscountType(r.getDiscountType());
        v.setDiscountValue(r.getDiscountValue());
        validateDiscount(v);
        v.setStatus(incoming ? "INCOMING" : (stock > 0 ? "AVAILABLE" : "OUT_OF_STOCK"));
    }

    private String normalizeCatalog(String category, String value, String label) {
        String cleaned = CatalogFormats.clean(value);
        if (cleaned.isEmpty())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, label + " không được để trống");
        String v = CatalogFormats.canonical(category, cleaned); // định dạng kiểm tra LẠI ở đây, kể cả giá trị cũ trong danh mục
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
        return productVariantRepository.findByProductId(productId).stream()
                .filter(x -> !x.getId().equals(excludeId))
                .anyMatch(x -> same(x.getColor(), v.getColor()) && same(x.getStringType(), v.getStringType())
                        && same(x.getTension(), v.getTension()) && same(x.getWeightGrip(), v.getWeightGrip()));
    }

    private boolean same(String a, String b) {
        return a != null && b != null && a.trim().equalsIgnoreCase(b.trim());
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