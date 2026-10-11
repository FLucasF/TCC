package com.loja.checkout.strategy.shipping;

import com.loja.checkout.strategy.ShippingStrategy;
import com.loja.checkout.util.MoneyRounder;
import java.math.BigDecimal;

public class EconomicaShipping implements ShippingStrategy {
    @Override
    public BigDecimal calcularFrete(BigDecimal pesoTotal) {
        BigDecimal base = new BigDecimal("12.00");
        BigDecimal adicionalPorKg = new BigDecimal("2.00");
        BigDecimal frete = base.add(adicionalPorKg.multiply(pesoTotal));
        return MoneyRounder.round(frete);
    }

    @Override
    public Integer getPrazoEntregaDias() {
        return 7;
    }
}
