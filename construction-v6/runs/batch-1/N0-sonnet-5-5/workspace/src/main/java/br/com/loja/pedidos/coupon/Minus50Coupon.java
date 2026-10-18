package br.com.loja.pedidos.coupon;

import br.com.loja.pedidos.common.Money;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class Minus50Coupon implements Coupon {

    private static final BigDecimal MINIMUM = Money.of("300.00");

    @Override
    public String code() {
        return "MINUS50";
    }

    @Override
    public boolean applicable(CouponContext context) {
        return context.productsSubtotal().compareTo(MINIMUM) >= 0;
    }

    @Override
    public BigDecimal discount(CouponContext context) {
        return Money.of("50.00");
    }
}
