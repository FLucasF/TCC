package br.com.loja.pedidos.payment;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import br.com.loja.pedidos.common.Money;

@Component
public class BankSlipPayment implements PaymentMethod {

    private static final BigDecimal FEE = new BigDecimal("3.49");
    private static final BigDecimal MAX_TOTAL = new BigDecimal("1000.00");

    @Override
    public String code() {
        return "BANK_SLIP";
    }

    @Override
    public boolean allowsInstallments(int installments) {
        return installments == 1;
    }

    @Override
    public boolean availableFor(BigDecimal orderTotal) {
        return orderTotal.compareTo(MAX_TOTAL) <= 0;
    }

    @Override
    public PaymentCharge charge(PaymentContext context) {
        BigDecimal finalTotal = context.orderTotal().add(Money.round(FEE));
        return new PaymentCharge(finalTotal, finalTotal);
    }
}
