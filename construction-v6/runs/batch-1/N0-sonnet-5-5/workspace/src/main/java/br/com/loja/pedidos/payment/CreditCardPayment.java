package br.com.loja.pedidos.payment;

import br.com.loja.pedidos.common.Money;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

@Component
public class CreditCardPayment implements PaymentMethod {

    private static final int INTEREST_FREE_UP_TO = 3;
    private static final int MAX_INSTALLMENTS = 12;
    private static final BigDecimal MONTHLY_RATE = new BigDecimal("0.0199");

    @Override
    public String code() {
        return "CREDIT_CARD";
    }

    @Override
    public boolean allowsInstallments(int installments) {
        return installments >= 1 && installments <= MAX_INSTALLMENTS;
    }

    @Override
    public PaymentCharge charge(BigDecimal orderTotal, int installments, boolean couponInformed) {
        BigDecimal n = BigDecimal.valueOf(installments);
        if (installments <= INTEREST_FREE_UP_TO) {
            return new PaymentCharge(orderTotal, orderTotal.divide(n, 2, RoundingMode.HALF_EVEN));
        }
        // Price table: total × i ÷ (1 − (1 + i)^−n)
        MathContext mc = MathContext.DECIMAL128;
        BigDecimal discount = BigDecimal.ONE.divide(BigDecimal.ONE.add(MONTHLY_RATE).pow(installments, mc), mc);
        BigDecimal installment = Money.round(orderTotal.multiply(MONTHLY_RATE)
                .divide(BigDecimal.ONE.subtract(discount), mc));
        return new PaymentCharge(installment.multiply(n), installment);
    }
}
