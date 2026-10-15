package com.loja.orders.payment;

import java.math.BigDecimal;
import java.math.MathContext;

import org.springframework.stereotype.Component;

import com.loja.orders.common.Money;

/** Cartão de crédito: até 3x sem juros; de 4x a 12x com juros mensais pela tabela Price. */
@Component
public class CreditCardPayment implements PaymentMethod {

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
    public PaymentTerms terms(PaymentContext context) {
        BigDecimal total = context.orderTotal();
        int n = context.installments();
        if (n <= MAX_INTEREST_FREE) {
            return new PaymentTerms(total, total.divide(BigDecimal.valueOf(n), 2, Money.ROUNDING));
        }
        // parcela = total × taxa ÷ (1 − (1 + taxa)^−n)
        MathContext mc = MathContext.DECIMAL128;
        BigDecimal discountFactor = BigDecimal.ONE.divide(BigDecimal.ONE.add(MONTHLY_RATE).pow(n), mc);
        BigDecimal installment = Money.round(
                total.multiply(MONTHLY_RATE).divide(BigDecimal.ONE.subtract(discountFactor), mc));
        return new PaymentTerms(installment.multiply(BigDecimal.valueOf(n)), installment);
    }
}
