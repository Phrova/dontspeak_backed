package com.example.springbootnyano.repository;

import com.example.springbootnyano.entity.catalog.OptionCatalogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OptionCatalogRepository extends JpaRepository<OptionCatalogEntity, Long> {
    List<OptionCatalogEntity> findByCategoryOrderByIdAsc(String category);
    boolean existsByCategoryAndOptionValueIgnoreCase(String category, String optionValue);
}