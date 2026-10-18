package br.com.loja.payment;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

import org.springframework.stereotype.Component;

import br.com.loja.common.ApiException;
import br.com.loja.common.ErrorCode;
import br.com.loja.common.Money;

@Component
class CreditCardPayment implements PaymentOption {

    private static final int MAX_INSTALLMENTS = 12;
    private static final int MAX_INTEREST_FREE = 3;
    private static final BigDecimal MONTHLY_RATE = new BigDecimal("0.0199");
    private static final MathContext PRECISION = new MathContext(34, RoundingMode.HALF_EVEN);

    @Override
    public String code() {
        return "CREDIT_CARD";
    }

    @Override
    public void validateInstallments(int installments) {
        if (installments < 1 || installments > MAX_INSTALLMENTS) {
            throw new ApiException(ErrorCode.INVALID_INSTALLMENTS);
        }
    }

    @Override
    public PaymentCharge charge(PaymentContext context) {
        BigDecimal total = context.orderTotal();
        int n = context.installments();
        if (n <= MAX_INTEREST_FREE) {
            return new PaymentCharge(total, Money.round(total.divide(BigDecimal.valueOf(n), PRECISION)));
        }
        // Tabela Price: parcela = total * i / (1 - (1 + i)^-n)
        BigDecimal growth = BigDecimal.ONE.add(MONTHLY_RATE).pow(n, PRECISION);
        BigDecimal installment = Money.round(total.multiply(MONTHLY_RATE).multiply(growth)
                .divide(growth.subtract(BigDecimal.ONE), PRECISION));
        return new PaymentCharge(installment.multiply(BigDecimal.valueOf(n)), installment);
    }
}
