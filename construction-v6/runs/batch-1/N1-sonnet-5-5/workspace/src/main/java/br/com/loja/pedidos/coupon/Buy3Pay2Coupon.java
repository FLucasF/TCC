package br.com.loja.pedidos.coupon;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import br.com.loja.pedidos.common.Money;

@Component
public class Buy3Pay2Coupon implements Coupon {

    @Override
    public String code() {
        return "BUY3PAY2";
    }

    @Override
    public BigDecimal discount(CouponContext context) {
        BigDecimal discount = Money.ZERO;
        for (var line : context.lines()) {
            BigDecimal freeUnits = BigDecimal.valueOf(line.quantity() / 3);
            discount = discount.add(line.product().price().multiply(freeUnits));
        }
        return Money.round(discount);
    }
}
