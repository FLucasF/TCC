package com.loja.checkout.strategy.entrega;

import com.loja.checkout.service.MoneyRounder;
import java.math.BigDecimal;

public class ExpressaStrategy implements ModalidadeEntregaStrategy {
    @Override
    public BigDecimal calcularFrete(double pesoTotalKg) {
        double frete = 25.0 + (4.50 * pesoTotalKg);
        return MoneyRounder.round(frete);
    }

    @Override
    public int getPrazoEntregaDias() {
        return 2;
    }

    @Override
    public void validar(double pesoTotalKg) {
    }
}
