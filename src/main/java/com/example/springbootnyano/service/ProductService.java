package com.example.springbootnyano.service;

import com.example.springbootnyano.dto.request.product.ProductCreateRequest;
import com.example.springbootnyano.dto.request.product.ProductUpdateRequest;
import com.example.springbootnyano.dto.request.product.VariantRequest;
import com.example.springbootnyano.entity.ProductEntity;
import com.example.springbootnyano.entity.productvariant.ProductVariantEntity;

public interface ProductService {
    ProductEntity createProduct(ProductCreateRequest request);
    ProductEntity updateProduct(Long id, ProductUpdateRequest request);
    void deleteProduct(Long id);
    ProductVariantEntity addVariant(Long productId, VariantRequest request);
    ProductVariantEntity updateVariant(Long variantId, VariantRequest request);
    void deleteVariant(Long variantId);
}