package shop.domain.payment;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import shop.domain.Money;

public class CreditCard implements PaymentMethod {
    private static final int MAX_INSTALLMENTS = 12;
    private static final int MAX_INTEREST_FREE = 3;
    private static final BigDecimal MONTHLY_RATE = new BigDecimal("0.0199");
    private static final MathContext MC = new MathContext(30, RoundingMode.HALF_EVEN);

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
        BigDecimal n = BigDecimal.valueOf(installments);
        if (installments <= MAX_INTEREST_FREE) {
            return new PaymentCharge(orderTotal, Money.round(orderTotal.divide(n, MC)));
        }
        // Tabela Price: parcela = total * i / (1 - (1 + i)^-n)
        BigDecimal growth = BigDecimal.ONE.add(MONTHLY_RATE).pow(installments);
        BigDecimal denominator = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(growth, MC));
        BigDecimal installment = Money.round(orderTotal.multiply(MONTHLY_RATE).divide(denominator, MC));
        return new PaymentCharge(installment.multiply(n), installment);
    }
}
