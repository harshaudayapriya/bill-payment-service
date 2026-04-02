package com.hash.billpay.service;

import com.hash.billpay.client.ExchangeRateClient;
import com.hash.billpay.dto.ConvertedTransactionResponse;
import com.hash.billpay.dto.ExchangeRateEntry;
import com.hash.billpay.dto.PurchaseTransactionRequest;
import com.hash.billpay.dto.PurchaseTransactionResponse;
import com.hash.billpay.exception.DuplicateTransactionException;
import com.hash.billpay.exception.TransactionNotFoundException;
import com.hash.billpay.model.PurchaseTransaction;
import com.hash.billpay.repository.PurchaseTransactionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PurchaseTransactionServiceTest {

    @Mock
    private PurchaseTransactionRepository repository;

    @Mock
    private ExchangeRateClient exchangeRateClient;

    @InjectMocks
    private PurchaseTransactionService service;

    @Nested
    @DisplayName("createTransaction")
    class CreateTransaction {

        @Test
        @DisplayName("Should create and return a transaction")
        void shouldCreateTransaction() {
            UUID idempotencyKey = UUID.randomUUID();
            PurchaseTransactionRequest request = PurchaseTransactionRequest.builder()
                    .idempotencyKey(idempotencyKey)
                    .description("Electric bill")
                    .transactionDate(LocalDate.of(2025, 3, 15))
                    .purchaseAmount(new BigDecimal("150.755"))
                    .build();

            PurchaseTransaction savedEntity = PurchaseTransaction.builder()
                    .id(UUID.randomUUID())
                    .idempotencyKey(idempotencyKey)
                    .description("Electric bill")
                    .transactionDate(LocalDate.of(2025, 3, 15))
                    .purchaseAmount(new BigDecimal("150.76")) // rounded
                    .build();

            when(repository.existsByIdempotencyKey(idempotencyKey)).thenReturn(false);
            when(repository.save(any(PurchaseTransaction.class))).thenReturn(savedEntity);

            PurchaseTransactionResponse response = service.createTransaction(request);

            assertThat(response).isNotNull();
            assertThat(response.getDescription()).isEqualTo("Electric bill");
            assertThat(response.getPurchaseAmount()).isEqualByComparingTo(new BigDecimal("150.76"));
            verify(repository).existsByIdempotencyKey(idempotencyKey);
            verify(repository).save(any(PurchaseTransaction.class));
        }

        @Test
        @DisplayName("Should throw DuplicateTransactionException when idempotency key already exists")
        void shouldThrowWhenDuplicateIdempotencyKey() {
            UUID idempotencyKey = UUID.randomUUID();
            PurchaseTransactionRequest request = PurchaseTransactionRequest.builder()
                    .idempotencyKey(idempotencyKey)
                    .description("Electric bill")
                    .transactionDate(LocalDate.of(2025, 3, 15))
                    .purchaseAmount(new BigDecimal("150.75"))
                    .build();

            when(repository.existsByIdempotencyKey(idempotencyKey)).thenReturn(true);

            assertThatThrownBy(() -> service.createTransaction(request))
                    .isInstanceOf(DuplicateTransactionException.class)
                    .hasMessageContaining(idempotencyKey.toString());

            verify(repository).existsByIdempotencyKey(idempotencyKey);
            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getTransaction")
    class GetTransaction {

        @Test
        @DisplayName("Should return a transaction by ID")
        void shouldReturnTransaction() {
            UUID id = UUID.randomUUID();
            PurchaseTransaction entity = PurchaseTransaction.builder()
                    .id(id)
                    .description("Water bill")
                    .transactionDate(LocalDate.of(2025, 1, 20))
                    .purchaseAmount(new BigDecimal("45.00"))
                    .build();

            when(repository.findById(id)).thenReturn(Optional.of(entity));

            PurchaseTransactionResponse response = service.getTransaction(id);

            assertThat(response.getId()).isEqualTo(id);
            assertThat(response.getDescription()).isEqualTo("Water bill");
        }

        @Test
        @DisplayName("Should throw when transaction not found")
        void shouldThrowWhenNotFound() {
            UUID id = UUID.randomUUID();
            when(repository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getTransaction(id))
                    .isInstanceOf(TransactionNotFoundException.class)
                    .hasMessageContaining(id.toString());
        }
    }

    @Nested
    @DisplayName("getTransactionInCurrency")
    class GetTransactionInCurrency {

        @Test
        @DisplayName("Should convert transaction to target currency")
        void shouldConvertCurrency() {
            UUID id = UUID.randomUUID();
            PurchaseTransaction entity = PurchaseTransaction.builder()
                    .id(id)
                    .description("Insurance premium")
                    .transactionDate(LocalDate.of(2025, 3, 1))
                    .purchaseAmount(new BigDecimal("100.00"))
                    .idempotencyKey(id)
                    .build();

            ExchangeRateEntry rateEntry = ExchangeRateEntry.builder()
                    .country("Canada")
                    .currency("Dollar")
                    .countryCurrencyDesc("Canada-Dollar")
                    .exchangeRate(new BigDecimal("1.35"))
                    .effectiveDate(LocalDate.of(2025, 3, 1))
                    .build();

            when(repository.findByIdempotencyKey(id)).thenReturn(Optional.of(entity));
            when(exchangeRateClient.getExchangeRate("Canada-Dollar", LocalDate.of(2025, 3, 1)))
                    .thenReturn(rateEntry);

            ConvertedTransactionResponse response = service.getTransactionInCurrency(id, "Canada-Dollar");

            assertThat(response.getOriginalAmountUsd()).isEqualByComparingTo(new BigDecimal("100.00"));
            assertThat(response.getExchangeRate()).isEqualByComparingTo(new BigDecimal("1.35"));
            assertThat(response.getConvertedAmount()).isEqualByComparingTo(new BigDecimal("135.00"));
            assertThat(response.getTargetCurrency()).isEqualTo("Canada-Dollar");
        }

        @Test
        @DisplayName("Should throw when transaction not found for conversion")
        void shouldThrowWhenTransactionNotFoundForConversion() {
            UUID id = UUID.randomUUID();
            when(repository.findByIdempotencyKey(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getTransactionInCurrency(id, "Canada-Dollar"))
                    .isInstanceOf(TransactionNotFoundException.class);
        }
    }
}
