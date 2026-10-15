package com.loja.orders.coupon;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.loja.orders.common.ApiException;
import com.loja.orders.common.ErrorCode;
import com.loja.orders.common.Money;

@Component
public class Coupons {

    /** Nenhum cupom dá desconto maior que esta parte do valor dos produtos. */
    private static final BigDecimal MAX_DISCOUNT_RATE = new BigDecimal("0.30");

    private final Map<String, Coupon> byCode;

    public Coupons(List<Coupon> coupons) {
        this.byCode = coupons.stream().collect(Collectors.toMap(Coupon::code, Function.identity()));
    }

    public Optional<Coupon> find(String code) {
        return Optional.ofNullable(code).map(byCode::get);
    }

    /** O desconto do cupom já limitado ao teto. */
    public BigDecimal discount(Coupon coupon, CouponContext context) {
        BigDecimal cap = Money.percentOf(context.productsSubtotal(), MAX_DISCOUNT_RATE);
        return Money.min(Money.round(coupon.discount(context)), cap);
    }

    static ApiException notApplicable() {
        return ApiException.unprocessable(ErrorCode.COUPON_NOT_APPLICABLE);
    }
}
