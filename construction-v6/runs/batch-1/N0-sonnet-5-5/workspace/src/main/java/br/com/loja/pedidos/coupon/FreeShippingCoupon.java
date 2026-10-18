package br.com.loja.pedidos.coupon;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class FreeShippingCoupon implements Coupon {

    @Override
    public String code() {
        return "FREESHIPPING";
    }

    @Override
    public boolean applicable(CouponContext context) {
        return !"STORE_PICKUP".equals(context.shippingMethod());
    }

    @Override
    public BigDecimal discount(CouponContext context) {
        return context.shippingCost();
    }
}
