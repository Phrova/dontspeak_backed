package com.example.springbootnyano.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "java_order_detail_001")
public class OrderDetailEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "order_id")
    @JsonIgnore
    private OrderEntity order;

    private Long productVariantId;
    private String productName;
    private String variantInfo;
    private BigDecimal price;
    private Integer quantity;
}