package br.com.loja.payment;

import java.math.BigDecimal;

/** Uma forma de pagamento. Nova forma = novo bean que implemente esta interface. */
public interface PaymentOption {

    String code();

    /** Lança {@code ApiException} INVALID_INSTALLMENTS se o número de parcelas não é permitido. */
    void validateInstallments(int installments);

    /** Lança {@code ApiException} PAYMENT_METHOD_UNAVAILABLE se a forma não atende o pedido. */
    default void validateAvailability(BigDecimal orderTotal) {
    }

    PaymentCharge charge(PaymentContext context);

    record PaymentContext(BigDecimal orderTotal, int installments, boolean couponInformed) {
    }

    record PaymentCharge(BigDecimal finalTotal, BigDecimal installmentAmount) {
    }
}
