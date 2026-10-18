package br.com.loja.pedidos.coupon;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import br.com.loja.pedidos.common.DomainException;

@Component
public class FreeShippingCoupon implements Coupon {

    @Override
    public String code() {
        return "FREESHIPPING";
    }

    @Override
    public BigDecimal discount(CouponContext context) {
        if (!context.shipping().chargesShipping()) {
            throw DomainException.unprocessable("COUPON_NOT_APPLICABLE");
        }
        return context.shippingCost();
    }
}
