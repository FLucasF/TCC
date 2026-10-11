package com.loja.checkout.strategy.shipping;

import com.loja.checkout.strategy.ShippingStrategy;
import com.loja.checkout.util.MoneyRounder;
import java.math.BigDecimal;

public class RetiradaLojaShipping implements ShippingStrategy {
    @Override
    public BigDecimal calcularFrete(BigDecimal pesoTotal) {
        return MoneyRounder.round(BigDecimal.ZERO);
    }

    @Override
    public Integer getPrazoEntregaDias() {
        return 1;
    }
}
