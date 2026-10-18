package br.com.loja.payment;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import br.com.loja.common.ApiException;
import br.com.loja.common.ErrorCode;
import br.com.loja.common.Money;

@Component
class BankSlipPayment extends CashPayment {

    private static final BigDecimal LIMIT = Money.of("1000.00");
    private static final BigDecimal FEE = Money.of("3.49");

    @Override
    public String code() {
        return "BANK_SLIP";
    }

    @Override
    public void validateAvailability(BigDecimal orderTotal) {
        if (orderTotal.compareTo(LIMIT) > 0) {
            throw new ApiException(ErrorCode.PAYMENT_METHOD_UNAVAILABLE);
        }
    }

    @Override
    public PaymentCharge charge(PaymentContext context) {
        var finalTotal = context.orderTotal().add(FEE);
        return new PaymentCharge(finalTotal, finalTotal);
    }
}
