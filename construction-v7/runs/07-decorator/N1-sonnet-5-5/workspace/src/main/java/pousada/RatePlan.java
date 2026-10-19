package pousada;

import java.math.BigDecimal;

import static pousada.Money.round;

public enum RatePlan {
    STANDARD("0", true),
    NON_REFUNDABLE("0.15", false);

    private final BigDecimal discountRate;
    private final boolean refundable;

    RatePlan(String discountRate, boolean refundable) {
        this.discountRate = new BigDecimal(discountRate);
        this.refundable = refundable;
    }

    public BigDecimal discountOn(BigDecimal subtotal) {
        return round(subtotal.multiply(discountRate));
    }

    public BigDecimal refundOn(BigDecimal amountPaid) {
        return refundable ? amountPaid : round(BigDecimal.ZERO);
    }
}
