package shop.domain.payment;

import java.math.BigDecimal;
import shop.domain.Money;

public class Pix implements PaymentMethod {
    private static final BigDecimal DISCOUNT_PERCENT = new BigDecimal("5");

    @Override
    public String code() {
        return "PIX";
    }

    @Override
    public boolean acceptsInstallments(int installments) {
        return installments == 1;
    }

    @Override
    public boolean availableFor(BigDecimal orderTotal) {
        return true;
    }

    @Override
    public PaymentCharge charge(BigDecimal orderTotal, int installments, boolean couponInformed) {
        BigDecimal total = couponInformed ? orderTotal : orderTotal.subtract(Money.percent(orderTotal, DISCOUNT_PERCENT));
        return new PaymentCharge(total, total);
    }
}
