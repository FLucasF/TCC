package com.loja.service.coupon;

import com.loja.exception.CheckoutException;
import com.loja.util.MoneyRounder;
import java.math.BigDecimal;

public class Menos50CouponCalculator implements CouponCalculator {
    private static final BigDecimal DISCOUNT = new BigDecimal("50.00");
    private static final BigDecimal MIN_SUBTOTAL = new BigDecimal("300.00");

    @Override
    public void validate(BigDecimal subtotalProdutos, BigDecimal pesoTotal) {
        if (subtotalProdutos.compareTo(MIN_SUBTOTAL) < 0) {
            throw new CheckoutException("CUPOM_NAO_APLICAVEL");
        }
    }

    @Override
    public BigDecimal calculate(BigDecimal subtotalProdutos, BigDecimal pesoTotal, BigDecimal frete) {
        return MoneyRounder.round(DISCOUNT);
    }
}
