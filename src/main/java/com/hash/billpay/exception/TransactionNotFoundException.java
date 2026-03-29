package com.hash.billpay.exception;

import java.util.UUID;

/**
 * Thrown when a purchase transaction is not found by its ID.
 */
public class TransactionNotFoundException extends RuntimeException {

    public TransactionNotFoundException(UUID id) {
        super("Purchase transaction not found with id: " + id);
    }
}