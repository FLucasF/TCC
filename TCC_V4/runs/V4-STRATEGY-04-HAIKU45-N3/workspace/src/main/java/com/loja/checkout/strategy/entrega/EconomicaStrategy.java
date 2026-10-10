package com.loja.checkout.strategy.entrega;

import com.loja.checkout.service.MoneyRounder;
import java.math.BigDecimal;

public class EconomicaStrategy implements ModalidadeEntregaStrategy {
    @Override
    public BigDecimal calcularFrete(double pesoTotalKg) {
        double frete = 12.0 + (2.0 * pesoTotalKg);
        return MoneyRounder.round(frete);
    }

    @Override
    public int getPrazoEntregaDias() {
        return 7;
    }

    @Override
    public void validar(double pesoTotalKg) {
    }
}
