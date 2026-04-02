package com.hash.billpay.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.data.domain.Auditable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.UUID;

/**
 * JPA entity representing a purchase transaction (bill payment).
 * Extends {@link Auditable} for automatic {@code createdAt}, {@code updatedAt},
 * {@code createdBy}, and {@code updatedBy} tracking.
 */
@Entity
@Table(name = "purchase_transaction", uniqueConstraints = {
        @UniqueConstraint(name = "uk_idempotency_key", columnNames = "idempotency_key")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class PurchaseTransaction extends BaseAuditEntity {

    /**
     * Unique identifier for the purchase transaction.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * Client-supplied idempotency key to prevent duplicate transactions.
     */
    @Column(name = "idempotency_key", nullable = false, unique = true, updatable = false)
    private UUID idempotencyKey;

    /**
     * Description of the transaction (max 50 characters).
     */
    @NotBlank(message = "Description is required")
    @Size(max = 50, message = "Description must not exceed 50 characters")
    @Column(name = "description", nullable = false, length = 50)
    private String description;

    /**
     * Date of the transaction.
     */
    @NotNull(message = "Transaction date is required")
    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    /**
     * Purchase amount in USD, rounded to the nearest cent.
     */
    @NotNull(message = "Purchase amount is required")
    @DecimalMin(value = "0.01", message = "Purchase amount must be a positive value")
    @Digits(integer = 15, fraction = 2, message = "Purchase amount must be rounded to the nearest cent")
    @Column(name = "purchase_amount", nullable = false, precision = 17, scale = 2)
    private BigDecimal purchaseAmount;


    /**
     * Ensures the purchase amount is rounded to the nearest cent before persisting.
     */
    @PrePersist
    @PreUpdate
    public void roundAmount() {
        if (purchaseAmount != null) {
            purchaseAmount = purchaseAmount.setScale(2, RoundingMode.HALF_UP);
        }
    }
}
