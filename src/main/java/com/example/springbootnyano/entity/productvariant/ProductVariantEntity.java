package com.example.springbootnyano.entity.productvariant;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "java_product_variant_001")
public class ProductVariantEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id")
    private Long productId;

    private String color;
    private String stringType;
    private String tension;
    private String weightGrip;
    private BigDecimal price;

    @Column(name = "stock_quantity")
    private Integer stockQuantity;

    private String discountType;
    private BigDecimal discountValue;
}