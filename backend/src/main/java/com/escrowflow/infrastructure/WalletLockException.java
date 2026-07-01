package com.escrowflow.infrastructure;

public class WalletLockException extends RuntimeException {

    public WalletLockException(String message) {
        super(message);
    }
}
