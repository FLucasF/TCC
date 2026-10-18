package br.com.loja.pedidos.payment;

import br.com.loja.pedidos.common.Money;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class PixPayment implements PaymentMethod {

    private static final BigDecimal DISCOUNT = new BigDecimal("0.05");

    @Override
    public String code() {
        return "PIX";
    }

    @Override
    public boolean allowsInstallments(int installments) {
        return installments == 1;
    }

    @Override
    public PaymentCharge charge(BigDecimal orderTotal, int installments, boolean couponInformed) {
        BigDecimal total = couponInformed ? orderTotal : orderTotal.subtract(Money.percent(orderTotal, DISCOUNT));
        return new PaymentCharge(total, total);
    }
}
