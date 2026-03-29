package com.hash.billpay.controller;

import com.hash.billpay.dto.ConvertedTransactionResponse;
import com.hash.billpay.dto.PurchaseTransactionRequest;
import com.hash.billpay.dto.PurchaseTransactionResponse;
import com.hash.billpay.service.PurchaseTransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@Tag(name = "Purchase Transactions", description = "Bill payment purchase transaction management")
public class PurchaseTransactionController {

    private final PurchaseTransactionService transactionService;

    /**
     * POST /api/v1/transactions
     * Creates a new purchase transaction.
     */
    @PostMapping
    @Operation(summary = "Create a purchase transaction",
            description = "Accepts a bill payment purchase transaction and stores it in the database")
    public ResponseEntity<PurchaseTransactionResponse> createTransaction(
            @Valid @RequestBody PurchaseTransactionRequest request) {
        PurchaseTransactionResponse response = transactionService.createTransaction(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/v1/transactions/{id}
     * Retrieves a purchase transaction by its unique identifier.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get a purchase transaction",
            description = "Retrieves a purchase transaction by its unique identifier")
    public ResponseEntity<PurchaseTransactionResponse> getTransaction(
            @PathVariable UUID id) {
        return ResponseEntity.ok(transactionService.getTransaction(id));
    }

    /**
     * GET /api/v1/transactions
     * Retrieves all purchase transactions with pagination.
     */
    @GetMapping
    @Operation(summary = "List all purchase transactions",
            description = "Retrieves all purchase transactions with pagination support")
    public ResponseEntity<Page<PurchaseTransactionResponse>> getAllTransactions(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(transactionService.getAllTransactions(pageable));
    }

    /**
     * GET /api/v1/transactions/{id}/convert?currency=Canada-Dollar
     * Retrieves the transaction with the amount converted to the specified currency.
     * The exchange rate is fetched from the Treasury Fiscal Data API, using the rate
     * closest to the transaction date.
     */
    @GetMapping("/{id}/convert")
    @Operation(summary = "Get transaction in a specified currency",
            description = "Retrieves a purchase transaction with the amount converted to the target currency "
                    + "using the exchange rate closest to the transaction date from the Treasury Fiscal Data API")
    public ResponseEntity<ConvertedTransactionResponse> getTransactionInCurrency(
            @PathVariable UUID id,
            @Parameter(description = "Target currency description, e.g. 'Canada-Dollar', 'Euro Zone-Euro', 'United Kingdom-Pound'")
            @RequestParam("currency") String currency) {
        return ResponseEntity.ok(transactionService.getTransactionInCurrency(id, currency));
    }
}