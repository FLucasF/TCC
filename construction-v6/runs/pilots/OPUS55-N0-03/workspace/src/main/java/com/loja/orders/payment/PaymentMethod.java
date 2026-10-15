package com.loja.orders.payment;

/**
 * Uma forma de pagamento. Para criar uma nova, basta um novo {@code @Component} que implemente esta
 * interface.
 */
public interface PaymentMethod {

    /** O código usado pelo site, como {@code PIX}. */
    String code();

    boolean acceptsInstallments(int installments);

    /**
     * O valor final e as parcelas, ou recusa com {@code PAYMENT_METHOD_UNAVAILABLE} se a forma de
     * pagamento não atende o pedido.
     */
    PaymentTerms terms(PaymentContext context);
}
