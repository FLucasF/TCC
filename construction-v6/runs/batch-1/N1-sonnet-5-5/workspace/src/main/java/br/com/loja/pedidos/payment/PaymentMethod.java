package br.com.loja.pedidos.payment;

import java.math.BigDecimal;

import br.com.loja.pedidos.common.Coded;

/** Uma forma de pagamento. Cada forma nova entra como uma nova implementação. */
public interface PaymentMethod extends Coded {

    boolean allowsInstallments(int installments);

    /** Falso quando a forma de pagamento não atende um pedido desse valor. */
    default boolean availableFor(BigDecimal orderTotal) {
        return true;
    }

    PaymentCharge charge(PaymentContext context);

    record PaymentContext(BigDecimal orderTotal, int installments, boolean couponInformed) {
    }

    record PaymentCharge(BigDecimal finalTotal, BigDecimal installmentAmount) {
    }
}
