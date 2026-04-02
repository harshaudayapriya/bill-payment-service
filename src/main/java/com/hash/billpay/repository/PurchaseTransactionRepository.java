package com.hash.billpay.repository;

import com.hash.billpay.model.PurchaseTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PurchaseTransactionRepository extends JpaRepository<PurchaseTransaction, UUID> {

    boolean existsByIdempotencyKey(UUID idempotencyKey);

    Page<PurchaseTransaction> findByTransactionDateBetween(LocalDate from, LocalDate to, Pageable pageable);


    Optional<PurchaseTransaction> findByIdempotencyKey(UUID id);
}
