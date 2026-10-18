package br.com.loja.coupon;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import br.com.loja.common.ApiException;
import br.com.loja.common.ErrorCode;
import br.com.loja.common.Money;

@Component
class Minus50Coupon implements Coupon {

    private static final BigDecimal MINIMUM = Money.of("300.00");

    @Override
    public String code() {
        return "MINUS50";
    }

    @Override
    public BigDecimal discount(CouponContext context) {
        if (context.productsSubtotal().compareTo(MINIMUM) < 0) {
            throw new ApiException(ErrorCode.COUPON_NOT_APPLICABLE);
        }
        return Money.of("50.00");
    }
}
