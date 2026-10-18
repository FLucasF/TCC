package shop.payment;

import shop.Money;

import java.math.BigDecimal;
import java.math.MathContext;

public final class CreditCardPayment implements PaymentOption {
    private static final int MAX_INSTALLMENTS = 12;
    private static final int MAX_INTEREST_FREE = 3;
    private static final BigDecimal MONTHLY_RATE = new BigDecimal("0.0199");

    @Override
    public String code() {
        return "CREDIT_CARD";
    }

    @Override
    public boolean acceptsInstallments(int installments) {
        return installments >= 1 && installments <= MAX_INSTALLMENTS;
    }

    @Override
    public boolean availableFor(BigDecimal orderTotal) {
        return true;
    }

    @Override
    public PaymentCharge charge(BigDecimal orderTotal, int installments, boolean couponInformed) {
        BigDecimal count = BigDecimal.valueOf(installments);
        if (installments <= MAX_INTEREST_FREE) {
            return new PaymentCharge(orderTotal, Money.round(orderTotal.divide(count, MathContext.DECIMAL128)));
        }
        // Price table: total * i / (1 - (1 + i)^-n)
        BigDecimal growth = BigDecimal.ONE.add(MONTHLY_RATE).pow(installments);
        BigDecimal divisor = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(growth, MathContext.DECIMAL128));
        BigDecimal installment = Money.round(orderTotal.multiply(MONTHLY_RATE).divide(divisor, MathContext.DECIMAL128));
        return new PaymentCharge(installment.multiply(count), installment);
    }
}
