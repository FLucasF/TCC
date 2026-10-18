package shop.payment;

import java.math.BigDecimal;

/** A way of paying: which installments it allows, when it is available and how it changes the total. */
public interface PaymentOption {
    String code();

    boolean acceptsInstallments(int installments);

    boolean availableFor(BigDecimal orderTotal);

    PaymentCharge charge(BigDecimal orderTotal, int installments, boolean couponInformed);
}
