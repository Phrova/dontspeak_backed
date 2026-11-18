package com.example.springbootnyano.controller.product;

import com.example.springbootnyano.entity.ProductEntity;
import com.example.springbootnyano.entity.productvariant.ProductVariantEntity;
import com.example.springbootnyano.repository.ProductRepository;
import com.example.springbootnyano.repository.ProductVariantRepository;
import com.example.springbootnyano.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/product")
public class ProductController {

    @Autowired private ProductService productService;

    @GetMapping("/stock")
    public Map<Long, Integer> getAllStock() {
        Map<Long, Integer> stockMap = new HashMap<>();
        List<ProductVariantEntity> variants = productVariantRepository.findAll();
        for (ProductVariantEntity v : variants) {
            if (v.getProductId() != null) {
                stockMap.put(v.getProductId(), stockMap.getOrDefault(v.getProductId(), 0) + v.getStockQuantity());
            }
        }
        return stockMap;
    }

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductVariantRepository productVariantRepository;

    @GetMapping("/getAll")
    public List<ProductEntity> getAllProducts() { return productService.listProductsDecorated(); }

    @GetMapping("/variants/{productId}")
    public List<ProductVariantEntity> getVariants(@PathVariable Long productId) {
        return productService.listVariantsDecorated(productId);
    }
}