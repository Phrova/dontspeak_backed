package com.example.springbootnyano.repository;

import com.example.springbootnyano.entity.voucher.VoucherEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface VoucherRepository extends JpaRepository<VoucherEntity, Long> {
    List<VoucherEntity> findByDeletedFalse();
    Optional<VoucherEntity> findByIdAndDeletedFalse(Long id);
    boolean existsByCodeIgnoreCaseAndDeletedFalse(String code);
}