package br.com.loja.pedidos.payment;

import java.math.BigDecimal;

/** A way of paying. Each method knows its installments, limitations and price adjustment. */
public interface PaymentMethod {

    String code();

    boolean allowsInstallments(int installments);

    /** Whether the method can be used for an order with this total (before the payment adjustment). */
    default boolean availableFor(BigDecimal orderTotal) {
        return true;
    }

    PaymentCharge charge(BigDecimal orderTotal, int installments, boolean couponInformed);
}
