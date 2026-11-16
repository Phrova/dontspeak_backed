package com.example.springbootnyano.entity.catalog;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "java_option_001",
        uniqueConstraints = @UniqueConstraint(columnNames = {"category", "option_value"}))
public class OptionCatalogEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String category;

    @Column(name = "option_value", nullable = false, length = 100)
    private String optionValue;
}