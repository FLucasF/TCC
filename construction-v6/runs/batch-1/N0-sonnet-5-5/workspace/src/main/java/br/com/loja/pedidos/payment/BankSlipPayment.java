package br.com.loja.pedidos.payment;

import br.com.loja.pedidos.common.Money;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class BankSlipPayment implements PaymentMethod {

    private static final BigDecimal FEE = Money.of("3.49");
    private static final BigDecimal LIMIT = Money.of("1000.00");

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
        return orderTotal.compareTo(LIMIT) <= 0;
    }

    @Override
    public PaymentCharge charge(BigDecimal orderTotal, int installments, boolean couponInformed) {
        BigDecimal total = orderTotal.add(FEE);
        return new PaymentCharge(total, total);
    }
}
