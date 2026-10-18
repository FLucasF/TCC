package shop.domain;

public enum ErrorCode {
    INVALID_ORDER(422),
    UNKNOWN_PRODUCT(422),
    PRODUCT_DISCONTINUED(422),
    INSUFFICIENT_STOCK(422),
    INVALID_CLUB_TIER(422),
    INVALID_REGION(422),
    INVALID_ZIP_CODE(422),
    INVALID_SHIPPING_METHOD(422),
    SHIPPING_METHOD_UNAVAILABLE(422),
    CARRIER_UNAVAILABLE(503),
    INVALID_COUPON(422),
    COUPON_NOT_APPLICABLE(422),
    INVALID_PAYMENT_METHOD(422),
    INVALID_INSTALLMENTS(422),
    PAYMENT_METHOD_UNAVAILABLE(422),
    ORDER_NOT_FOUND(404),
    OPERATION_NOT_ALLOWED(409),
    ORDER_EXPIRED(409),
    PRODUCT_NOT_FOUND(404),
    CUSTOMER_NOT_FOUND(404),
    NOTIFICATION_NOT_FOUND(404);

    private final int defaultStatus;

    ErrorCode(int defaultStatus) {
        this.defaultStatus = defaultStatus;
    }

    public int defaultStatus() {
        return defaultStatus;
    }
}
