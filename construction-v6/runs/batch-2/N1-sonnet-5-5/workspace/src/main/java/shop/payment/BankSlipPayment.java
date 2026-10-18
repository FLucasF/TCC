package shop.payment;

import shop.Money;

import java.math.BigDecimal;

public final class BankSlipPayment implements PaymentOption {
    private static final BigDecimal FEE = Money.of("3.49");
    private static final BigDecimal MAXIMUM_TOTAL = Money.of("1000.00");

    @Override
    public String code() {
        return "BANK_SLIP";
    }

    @Override
    public boolean acceptsInstallments(int installments) {
        return installments == 1;
    }

    @Override
    public boolean availableFor(BigDecimal orderTotal) {
        return orderTotal.compareTo(MAXIMUM_TOTAL) <= 0;
    }

    @Override
    public PaymentCharge charge(BigDecimal orderTotal, int installments, boolean couponInformed) {
        BigDecimal total = orderTotal.add(FEE);
        return new PaymentCharge(total, total);
    }
}
