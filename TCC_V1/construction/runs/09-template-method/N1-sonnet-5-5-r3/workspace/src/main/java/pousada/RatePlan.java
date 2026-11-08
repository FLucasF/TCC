package pousada;

import java.math.BigDecimal;

/** Cada plano de tarifa define só o seu desconto e se devolve o pago no cancelamento. */
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
        return Money.round(subtotal.multiply(discountRate));
    }

    public BigDecimal refundFor(BigDecimal amountPaid) {
        return refundable ? amountPaid : Money.ZERO;
    }
}
