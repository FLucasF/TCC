package br.com.loja.coupon;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import br.com.loja.common.ApiException;
import br.com.loja.common.ErrorCode;

@Component
class FreeShippingCoupon implements Coupon {

    @Override
    public String code() {
        return "FREESHIPPING";
    }

    @Override
    public BigDecimal discount(CouponContext context) {
        if (!context.shipping().chargesFreight()) {
            throw new ApiException(ErrorCode.COUPON_NOT_APPLICABLE);
        }
        return context.shippingCost();
    }
}
