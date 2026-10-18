package br.com.loja.pedidos.coupon;

import br.com.loja.pedidos.checkout.PricedItem;
import br.com.loja.pedidos.common.Money;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class Buy3Pay2Coupon implements Coupon {

    @Override
    public String code() {
        return "BUY3PAY2";
    }

    @Override
    public BigDecimal discount(CouponContext context) {
        BigDecimal total = Money.ZERO;
        for (PricedItem item : context.items()) {
            total = total.add(item.unitPrice().multiply(BigDecimal.valueOf(item.quantity() / 3)));
        }
        return Money.round(total);
    }
}
