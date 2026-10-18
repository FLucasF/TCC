package shop.payment;

import shop.Money;

import java.math.BigDecimal;

public final class PixPayment implements PaymentOption {
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
        BigDecimal discount = couponInformed ? Money.ZERO : Money.round(orderTotal.multiply(new BigDecimal("0.05")));
        BigDecimal total = orderTotal.subtract(discount);
        return new PaymentCharge(total, total);
    }
}
