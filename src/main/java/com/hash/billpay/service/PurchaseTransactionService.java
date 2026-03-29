package com.hash.billpay.service;

import com.hash.billpay.client.ExchangeRateClient;
import com.hash.billpay.dto.ConvertedTransactionResponse;
import com.hash.billpay.dto.ExchangeRateEntry;
import com.hash.billpay.dto.PurchaseTransactionRequest;
import com.hash.billpay.dto.PurchaseTransactionResponse;
import com.hash.billpay.exception.TransactionNotFoundException;
import com.hash.billpay.model.PurchaseTransaction;
import com.hash.billpay.repository.PurchaseTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@RequiredArgsConstructor
@Slf4j
@Service
public class PurchaseTransactionService {

    private final ExchangeRateClient exchangeRateClient;
    private final PurchaseTransactionRepository repository;

    /**
     * Creates and persists a new purchase transaction.
     */
    @Transactional
    public PurchaseTransactionResponse createTransaction(PurchaseTransactionRequest request) {

        PurchaseTransaction transaction = PurchaseTransaction.builder()
                .description(request.getDescription())
                .transactionDate(request.getTransactionDate())
                .purchaseAmount(request.getPurchaseAmount().setScale(2, RoundingMode.HALF_UP))
                .billerType(request.getBillerType())
                .build();

        PurchaseTransaction saved = repository.save(transaction);
        log.info("Created purchase transaction: {}", saved.getId());
        return toResponse(saved);
    }

    /**
     * Retrieves a transaction by its unique identifier.
     */
    @Transactional(readOnly = true)
    public PurchaseTransactionResponse getTransaction(UUID id) {

        PurchaseTransaction transaction = repository.findById(id)
                .orElseThrow(() -> new TransactionNotFoundException(id));
        return toResponse(transaction);
    }


    /**
     * Retrieves all transactions with pagination.
     */
    @Transactional(readOnly = true)
    public Page<PurchaseTransactionResponse> getAllTransactions(Pageable pageable) {
        return repository.findAll(pageable).map(this::toResponse);
    }


    /**
     * Retrieves a transaction by its unique identifier and converts the purchase amount to the specified target currency.
     * <p>
     * The exchange rate is fetched from the Treasury Fiscal Data API and the rate with
     * the closest effective date to the transaction date is used.
     * </p>
     *
     * @param id             the unique identifier of the purchase transaction
     * @param targetCurrency the target currency description (e.g., "Canada-Dollar", "Euro Zone-Euro")
     * @return the transaction with converted amount
     */
    @Transactional(readOnly = true)
    public ConvertedTransactionResponse getTransactionInCurrency(UUID id, String targetCurrency) {

        PurchaseTransaction transaction = repository.findById(id)
                .orElseThrow(() -> new TransactionNotFoundException(id));

        // Fetch the closest exchange rate from the external API
        ExchangeRateEntry rateEntry = exchangeRateClient.getExchangeRate(
                targetCurrency, transaction.getTransactionDate());
        // Convert: convertedAmount = originalAmount * exchangeRate
        BigDecimal convertedAmount = transaction.getPurchaseAmount()
                .multiply(rateEntry.getExchangeRate())
                .setScale(2, RoundingMode.HALF_UP);
        log.info("Converted transaction {} to {} | rate={} | converted={}",
                id, targetCurrency, rateEntry.getExchangeRate(), convertedAmount);

        return ConvertedTransactionResponse.builder()
                .id(transaction.getId())
                .description(transaction.getDescription())
                .transactionDate(transaction.getTransactionDate())
                .billerType(transaction.getBillerType())
                .originalAmountUsd(transaction.getPurchaseAmount())
                .exchangeRate(rateEntry.getExchangeRate())
                .convertedAmount(convertedAmount)
                .targetCurrency(targetCurrency)
                .exchangeRateDate(rateEntry.getEffectiveDate())
                .build();
    }

    private PurchaseTransactionResponse toResponse(PurchaseTransaction entity) {
        return PurchaseTransactionResponse.builder()
                .id(entity.getId())
                .description(entity.getDescription())
                .transactionDate(entity.getTransactionDate())
                .purchaseAmount(entity.getPurchaseAmount())
                .billerType(entity.getBillerType())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
