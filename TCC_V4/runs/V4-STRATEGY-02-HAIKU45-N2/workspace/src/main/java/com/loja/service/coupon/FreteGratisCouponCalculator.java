package com.loja.service.coupon;

import com.loja.util.MoneyRounder;
import java.math.BigDecimal;

public class FreteGratisCouponCalculator implements CouponCalculator {
    @Override
    public void validate(BigDecimal subtotalProdutos, BigDecimal pesoTotal) {
    }

    @Override
    public BigDecimal calculate(BigDecimal subtotalProdutos, BigDecimal pesoTotal, BigDecimal frete) {
        return MoneyRounder.round(frete);
    }
}
