package br.com.loja.pedidos.coupon;

import br.com.loja.pedidos.common.Money;
import br.com.loja.pedidos.order.OrderRepository;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class Welcome10Coupon implements Coupon {

    private static final BigDecimal RATE = new BigDecimal("0.10");

    private final OrderRepository orders;

    public Welcome10Coupon(OrderRepository orders) {
        this.orders = orders;
    }

    @Override
    public String code() {
        return "WELCOME10";
    }

    @Override
    public boolean applicable(CouponContext context) {
        return !orders.hasPaidOrder(context.cpf());
    }

    @Override
    public BigDecimal discount(CouponContext context) {
        return Money.percent(context.productsSubtotal(), RATE);
    }
}
