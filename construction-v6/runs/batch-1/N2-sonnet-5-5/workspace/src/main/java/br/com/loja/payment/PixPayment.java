package br.com.loja.payment;

import org.springframework.stereotype.Component;

import br.com.loja.common.Money;

@Component
class PixPayment extends CashPayment {

    @Override
    public String code() {
        return "PIX";
    }

    @Override
    public PaymentCharge charge(PaymentContext context) {
        var total = context.orderTotal();
        var finalTotal = context.couponInformed() ? total : total.subtract(Money.percent(total, "0.05"));
        return new PaymentCharge(finalTotal, finalTotal);
    }
}
