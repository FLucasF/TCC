package br.com.loja.coupon;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import br.com.loja.common.ApiException;
import br.com.loja.common.ErrorCode;
import br.com.loja.common.Money;

@Component
class Welcome10Coupon implements Coupon {

    @Override
    public String code() {
        return "WELCOME10";
    }

    @Override
    public BigDecimal discount(CouponContext context) {
        if (!context.firstPurchase()) {
            throw new ApiException(ErrorCode.COUPON_NOT_APPLICABLE);
        }
        return Money.percent(context.productsSubtotal(), "0.10");
    }
}
