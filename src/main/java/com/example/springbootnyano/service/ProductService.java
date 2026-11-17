package com.example.springbootnyano.service;

import com.example.springbootnyano.dto.request.product.*;
import com.example.springbootnyano.dto.response.product.BulkCreateResult;
import com.example.springbootnyano.entity.ProductEntity;
import com.example.springbootnyano.entity.productvariant.ProductVariantEntity;

import java.util.List;
import java.util.Map;

public interface ProductService {
    BulkCreateResult bulkCreateVariantsFromRows(Long productId, BulkRowsRequest request);
    Map<String, Object> deleteVariantsByFilter(Long productId, VariantFilterRequest filter);
    ProductEntity createProduct(ProductCreateRequest request);
    ProductEntity updateProduct(Long id, ProductUpdateRequest request);
    void deleteProduct(Long id);
    ProductVariantEntity addVariant(Long productId, VariantRequest request);
    ProductVariantEntity updateVariant(Long variantId, VariantRequest request);
    void deleteVariant(Long variantId);
    BulkCreateResult bulkCreateVariants(Long productId, BulkVariantRequest request);
    Map<String, Object> bulkDeleteProducts(List<Long> ids);
    Map<String, Object> bulkDeleteVariants(List<Long> ids);
}