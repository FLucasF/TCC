package com.loja.service.coupon;

import com.loja.util.MoneyRounder;
import java.math.BigDecimal;

public class BemVindo10CouponCalculator implements CouponCalculator {
    private static final BigDecimal DISCOUNT_RATE = new BigDecimal("0.10");

    @Override
    public void validate(BigDecimal subtotalProdutos, BigDecimal pesoTotal) {
    }

    @Override
    public BigDecimal calculate(BigDecimal subtotalProdutos, BigDecimal pesoTotal, BigDecimal frete) {
        BigDecimal discount = subtotalProdutos.multiply(DISCOUNT_RATE);
        return MoneyRounder.round(discount);
    }
}
