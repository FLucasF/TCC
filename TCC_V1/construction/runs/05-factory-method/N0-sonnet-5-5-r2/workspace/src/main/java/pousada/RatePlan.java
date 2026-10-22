package pousada;

import java.math.BigDecimal;

public enum RatePlan {
    STANDARD(BigDecimal.ZERO, true),
    NON_REFUNDABLE(new BigDecimal("0.15"), false);

    public final BigDecimal discountRate;
    public final boolean refundable;

    RatePlan(BigDecimal discountRate, boolean refundable) {
        this.discountRate = discountRate;
        this.refundable = refundable;
    }
}
