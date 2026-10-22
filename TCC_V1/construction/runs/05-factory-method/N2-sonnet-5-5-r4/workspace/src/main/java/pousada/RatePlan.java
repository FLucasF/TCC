package pousada;

import java.math.BigDecimal;

enum RatePlan {
    STANDARD("0", true),
    NON_REFUNDABLE("0.15", false);

    private final String discountRate;
    private final boolean refundable;

    RatePlan(String discountRate, boolean refundable) {
        this.discountRate = discountRate;
        this.refundable = refundable;
    }

    BigDecimal discountOn(BigDecimal subtotal) {
        return Money.percent(subtotal, discountRate);
    }

    BigDecimal refundFor(BigDecimal amountPaid) {
        return refundable ? amountPaid : Money.round(BigDecimal.ZERO);
    }
}
