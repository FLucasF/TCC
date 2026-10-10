package com.loja.service.payment;

import com.loja.exception.CheckoutException;
import com.loja.util.MoneyRounder;
import java.math.BigDecimal;

public class BoletoPaymentAdjuster implements PaymentAdjuster {
    private static final BigDecimal FEE = new BigDecimal("3.49");
    private static final BigDecimal MAX_TOTAL = new BigDecimal("1000.00");

    @Override
    public void validate(Integer parcelas, BigDecimal total) {
        if (parcelas != null && parcelas != 1) {
            throw new CheckoutException("PARCELAMENTO_INVALIDO");
        }
        if (total.compareTo(MAX_TOTAL) > 0) {
            throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
        }
    }

    @Override
    public PaymentAdjustment calculate(BigDecimal total, Integer parcelas) {
        BigDecimal finalTotal = total.add(FEE);
        finalTotal = MoneyRounder.round(finalTotal);
        BigDecimal ajuste = FEE;
        return new PaymentAdjustment(ajuste, 1, finalTotal);
    }
}
