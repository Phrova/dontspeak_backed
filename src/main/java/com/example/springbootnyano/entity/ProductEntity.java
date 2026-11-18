package com.example.springbootnyano.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

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

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @Transient private BigDecimal minPrice;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}