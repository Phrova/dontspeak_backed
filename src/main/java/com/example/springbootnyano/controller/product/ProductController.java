package com.example.springbootnyano.controller.product;

import com.example.springbootnyano.entity.ProductEntity;
import com.example.springbootnyano.entity.productvariant.ProductVariantEntity;
import com.example.springbootnyano.repository.ProductRepository;
import com.example.springbootnyano.repository.ProductVariantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/product")
public class ProductController {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductVariantRepository productVariantRepository;

    @GetMapping("/getAll")
    public List<ProductEntity> getAllProducts() {
        return productRepository.findAll();
    }

    @GetMapping("/variants/{productId}")
    public List<ProductVariantEntity> getVariants(@PathVariable Long productId) {
        return productVariantRepository.findByProductId(productId);
    }
}