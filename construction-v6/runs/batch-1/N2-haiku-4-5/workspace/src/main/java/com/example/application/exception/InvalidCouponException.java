package com.example.application.exception;

public class InvalidCouponException extends ApplicationException {
    public InvalidCouponException() {
        super("INVALID_COUPON", 422);
    }
}
