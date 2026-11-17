package com.example.springbootnyano.controller.product;

import com.example.springbootnyano.dto.request.product.*;
import com.example.springbootnyano.dto.response.product.BulkCreateResult;
import com.example.springbootnyano.entity.ProductEntity;
import com.example.springbootnyano.entity.productvariant.ProductVariantEntity;
import com.example.springbootnyano.service.ProductService;
import com.example.springbootnyano.util.AuthGuard;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/admin/product")
public class AdminProductController {

    @Autowired private AuthGuard guard;
    @Autowired private ProductService productService;

    @PostMapping("/{id}/variants/bulk-rows")
    public BulkCreateResult bulkCreateRows(HttpServletRequest request, @PathVariable Long id,
                                           @RequestBody BulkRowsRequest body) {
        guard.requireAdmin(request);
        return productService.bulkCreateVariantsFromRows(id, body);
    }

    @PostMapping("/{id}/variants/delete-by-filter")
    public Map<String, Object> deleteVariantsByFilter(HttpServletRequest request, @PathVariable Long id,
                                                      @RequestBody VariantFilterRequest body) {
        guard.requireAdmin(request);
        return productService.deleteVariantsByFilter(id, body);
    }

    @PostMapping("/create")
    public ProductEntity create(HttpServletRequest request, @RequestBody ProductCreateRequest body) {
        guard.requireAdmin(request);
        return productService.createProduct(body);
    }

    @PutMapping("/{id}")
    public ProductEntity update(HttpServletRequest request, @PathVariable Long id, @RequestBody ProductUpdateRequest body) {
        guard.requireAdmin(request);
        return productService.updateProduct(id, body);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(HttpServletRequest request, @PathVariable Long id) {
        guard.requireAdmin(request);
        productService.deleteProduct(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/variant")
    public ProductVariantEntity addVariant(HttpServletRequest request, @PathVariable Long id, @RequestBody VariantRequest body) {
        guard.requireAdmin(request);
        return productService.addVariant(id, body);
    }

    @PutMapping("/variant/{variantId}")
    public ProductVariantEntity updateVariant(HttpServletRequest request, @PathVariable Long variantId, @RequestBody VariantRequest body) {
        guard.requireAdmin(request);
        return productService.updateVariant(variantId, body);
    }

    @DeleteMapping("/variant/{variantId}")
    public ResponseEntity<Void> deleteVariant(HttpServletRequest request, @PathVariable Long variantId) {
        guard.requireAdmin(request);
        productService.deleteVariant(variantId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/variants/bulk")
    public BulkCreateResult bulkCreateVariants(HttpServletRequest request, @PathVariable Long id, @RequestBody BulkVariantRequest body) {
        guard.requireAdmin(request);
        return productService.bulkCreateVariants(id, body);
    }

    @PostMapping("/bulk-delete")
    public Map<String, Object> bulkDeleteProducts(HttpServletRequest request, @RequestBody BulkDeleteRequest body) {
        guard.requireAdmin(request);
        return productService.bulkDeleteProducts(body.getIds());
    }

    @PostMapping("/variant/bulk-delete")
    public Map<String, Object> bulkDeleteVariants(HttpServletRequest request, @RequestBody BulkDeleteRequest body) {
        guard.requireAdmin(request);
        return productService.bulkDeleteVariants(body.getIds());
    }
}