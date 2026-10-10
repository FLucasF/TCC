package com.loja.service.coupon;

import java.math.BigDecimal;

public interface CouponCalculator {
    void validate(BigDecimal subtotalProdutos, BigDecimal pesoTotal);
    BigDecimal calculate(BigDecimal subtotalProdutos, BigDecimal pesoTotal, BigDecimal frete);
}
