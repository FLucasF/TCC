package br.com.loja.pedidos.payment;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

import org.springframework.stereotype.Component;

import br.com.loja.pedidos.common.Money;

@Component
public class CreditCardPayment implements PaymentMethod {

    private static final int MAX_INSTALLMENTS = 12;
    private static final int MAX_INTEREST_FREE_INSTALLMENTS = 3;
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
    public PaymentCharge charge(PaymentContext context) {
        BigDecimal total = context.orderTotal();
        int n = context.installments();
        if (n <= MAX_INTEREST_FREE_INSTALLMENTS) {
            BigDecimal installment = Money.round(total.divide(BigDecimal.valueOf(n), 10, RoundingMode.HALF_EVEN));
            return new PaymentCharge(total, installment);
        }
        // tabela Price: parcela = total * i / (1 - (1 + i)^-n)
        MathContext mc = MathContext.DECIMAL128;
        BigDecimal growth = BigDecimal.ONE.add(MONTHLY_RATE).pow(n, mc);
        BigDecimal discountFactor = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(growth, mc), mc);
        BigDecimal installment = Money.round(total.multiply(MONTHLY_RATE, mc).divide(discountFactor, mc));
        return new PaymentCharge(installment.multiply(BigDecimal.valueOf(n)), installment);
    }
}
