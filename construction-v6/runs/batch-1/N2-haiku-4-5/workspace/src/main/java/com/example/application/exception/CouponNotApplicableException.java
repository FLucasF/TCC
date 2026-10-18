package com.example.application.exception;

public class CouponNotApplicableException extends ApplicationException {
    public CouponNotApplicableException() {
        super("COUPON_NOT_APPLICABLE", 422);
    }
}
