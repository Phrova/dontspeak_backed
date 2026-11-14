package com.example.springbootnyano.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@Table(name = "java_order_001")
public class OrderEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private String receiverName;
    private String receiverPhone;
    private String shippingAddress;
    private BigDecimal totalAmount;
    private String paymentMethod;
    private String orderType;
    private String orderStatus;
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<OrderDetailEntity> orderDetails;
}