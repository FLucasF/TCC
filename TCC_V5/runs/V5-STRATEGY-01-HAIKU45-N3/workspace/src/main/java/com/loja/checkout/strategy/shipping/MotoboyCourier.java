package com.loja.checkout.strategy.shipping;

import com.loja.checkout.strategy.ShippingStrategy;
import com.loja.checkout.util.MoneyRounder;
import java.math.BigDecimal;

public class MotoboyCourier implements ShippingStrategy {
    @Override
    public BigDecimal calcularFrete(BigDecimal pesoTotal) {
        return MoneyRounder.round(new BigDecimal("18.00"));
    }

    @Override
    public Integer getPrazoEntregaDias() {
        return 0;
    }
}
