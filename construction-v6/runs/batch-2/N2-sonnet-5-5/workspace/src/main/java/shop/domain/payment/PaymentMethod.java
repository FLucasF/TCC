package shop.domain.payment;

import java.math.BigDecimal;

/** Uma forma de pagamento. Uma nova forma é uma nova implementação registrada em {@link PaymentMethods}. */
public interface PaymentMethod {
    String code();

    boolean acceptsInstallments(int installments);

    /** Se a forma de pagamento atende um pedido com este total. */
    boolean availableFor(BigDecimal orderTotal);

    PaymentCharge charge(BigDecimal orderTotal, int installments, boolean couponInformed);

    record PaymentCharge(BigDecimal finalTotal, BigDecimal installmentAmount) {
    }
}
