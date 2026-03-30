package com.hash.billpay.exception;

import java.util.UUID;

/**
 * Thrown when a transaction with the same idempotency key already exists.
 */
public class DuplicateTransactionException extends RuntimeException {

    public DuplicateTransactionException(UUID idempotencyKey) {
        super("A transaction with idempotency key '" + idempotencyKey + "' already exists");
    }
}
