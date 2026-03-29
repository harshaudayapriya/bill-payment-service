package com.hash.billpay.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA entity representing a purchase transaction (bill payment).
 */
@Entity
@Table(name = "purchase_transaction")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseTransaction {

    /**
     * Unique identifier for the purchase transaction.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

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
     * Biller type / service provider category.
     */
    @NotNull(message = "Biller type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "biller_type", nullable = false)
    private BillerType billerType;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

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
