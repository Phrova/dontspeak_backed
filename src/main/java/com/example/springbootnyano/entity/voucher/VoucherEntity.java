package com.example.springbootnyano.entity.voucher;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "java_voucher_001")
public class VoucherEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(nullable = false, length = 10)
    private String discountType;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal discountValue;

    @Column(nullable = false, precision = 18, scale = 0)
    private BigDecimal minOrderValue;

    @Column(precision = 18, scale = 0)
    private BigDecimal maxDiscountAmount;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private Integer usedQuantity = 0;

    @Column(nullable = false)
    private LocalDateTime startAt;

    @Column(nullable = false)
    private LocalDateTime endAt;

    @Column(nullable = false, length = 10)
    private String status = "ACTIVE";

    @Column(length = 10)
    private String scope = "ORDER";

    @Column(nullable = false)
    private Boolean deleted = false;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @Transient
    private String effectiveStatus;



    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (usedQuantity == null) usedQuantity = 0;
        if (deleted == null) deleted = false;
    }
}