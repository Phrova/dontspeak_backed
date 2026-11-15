package com.example.springbootnyano.service.impl;

import com.example.springbootnyano.dto.request.product.ProductCreateRequest;
import com.example.springbootnyano.dto.request.product.ProductUpdateRequest;
import com.example.springbootnyano.dto.request.product.VariantRequest;
import com.example.springbootnyano.entity.ProductEntity;
import com.example.springbootnyano.entity.productvariant.ProductVariantEntity;
import com.example.springbootnyano.repository.ProductRepository;
import com.example.springbootnyano.repository.ProductVariantRepository;
import com.example.springbootnyano.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

@Service
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductVariantRepository productVariantRepository;

    @Override
    @Transactional
    public ProductEntity createProduct(ProductCreateRequest request) {
        ProductEntity product = new ProductEntity();
        product.setProductName(request.getProductName());
        product.setProductPrice(request.getProductPrice());
        product = productRepository.save(product);

        if (request.getVariants() != null) {
            for (VariantRequest vr : request.getVariants()) {
                productVariantRepository.save(toVariant(product.getId(), vr));
            }
        }
        return product;
    }

    @Override
    @Transactional
    public ProductEntity updateProduct(Long id, ProductUpdateRequest request) {
        ProductEntity product = productRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy sản phẩm " + id));
        product.setProductName(request.getProductName());
        product.setProductPrice(request.getProductPrice());
        return productRepository.save(product);
    }

    @Override
    @Transactional
    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy sản phẩm " + id);
        }
        // productId là cột thường (không có FK) nên phải tự xoá con trước để không mồ côi biến thể
        productVariantRepository.deleteAll(productVariantRepository.findByProductId(id));
        productRepository.deleteById(id);
    }

    @Override
    @Transactional
    public ProductVariantEntity addVariant(Long productId, VariantRequest request) {
        if (!productRepository.existsById(productId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy sản phẩm " + productId);
        }
        return productVariantRepository.save(toVariant(productId, request));
    }

    @Override
    @Transactional
    public ProductVariantEntity updateVariant(Long variantId, VariantRequest request) {
        ProductVariantEntity v = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy biến thể " + variantId));
        applyVariant(v, request);
        return productVariantRepository.save(v);
    }

    @Override
    @Transactional
    public void deleteVariant(Long variantId) {
        if (!productVariantRepository.existsById(variantId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy biến thể " + variantId);
        }
        productVariantRepository.deleteById(variantId);
    }

    private ProductVariantEntity toVariant(Long productId, VariantRequest request) {
        ProductVariantEntity v = new ProductVariantEntity();
        v.setProductId(productId);
        applyVariant(v, request);
        return v;
    }

    private void applyVariant(ProductVariantEntity v, VariantRequest r) {
        v.setColor(r.getColor());
        v.setStringType(r.getStringType());
        v.setTension(r.getTension());
        v.setWeightGrip(r.getWeightGrip());
        v.setPrice(r.getPrice());
        v.setStockQuantity(r.getStockQuantity() == null ? 0 : r.getStockQuantity());
        v.setDiscountType(r.getDiscountType());
        v.setDiscountValue(r.getDiscountValue());
        validateDiscount(v);
    }

    private void validateDiscount(ProductVariantEntity v) {
        if (v.getDiscountType() == null || v.getDiscountType().isBlank()) {
            v.setDiscountType(null);
            v.setDiscountValue(null);
            return;
        }
        String type = v.getDiscountType().trim().toUpperCase();
        if (!type.equals("PERCENT") && !type.equals("FIXED")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "discountType phải là PERCENT hoặc FIXED");
        }
        if (v.getDiscountValue() == null || v.getDiscountValue().signum() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "discountValue phải là số >= 0");
        }
        if (type.equals("PERCENT") && v.getDiscountValue().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Giảm theo PERCENT không được vượt quá 100");
        }
        v.setDiscountType(type);
    }
}