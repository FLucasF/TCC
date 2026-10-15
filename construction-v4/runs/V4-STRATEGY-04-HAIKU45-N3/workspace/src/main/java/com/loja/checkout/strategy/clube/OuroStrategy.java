package com.loja.checkout.strategy.clube;

import com.loja.checkout.service.MoneyRounder;
import java.math.BigDecimal;

public class OuroStrategy implements ClubeMemberStrategy {
    @Override
    public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
        return MoneyRounder.round(subtotalProdutos.multiply(BigDecimal.valueOf(0.05)));
    }

    @Override
    public boolean temFreteGratis() {
        return true;
    }

    @Override
    public boolean validarBrinde(BigDecimal subtotalProdutos) {
        return subtotalProdutos.compareTo(BigDecimal.valueOf(500.0)) > 0;
    }
}
