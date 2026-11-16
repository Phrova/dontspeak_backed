package com.example.springbootnyano.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "java_product_001",
        uniqueConstraints = @UniqueConstraint(name = "UQ_product_name", columnNames = {"product_name"}))
public class ProductEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String productName;

    private BigDecimal productPrice;

    private String brand;
}