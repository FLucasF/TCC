package com.loja.checkout.strategy.clube;

import com.loja.checkout.service.MoneyRounder;
import java.math.BigDecimal;

public class PrataStrategy implements ClubeMemberStrategy {
    @Override
    public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
        return MoneyRounder.round(subtotalProdutos.multiply(BigDecimal.valueOf(0.02)));
    }

    @Override
    public boolean temFreteGratis() {
        return false;
    }

    @Override
    public boolean validarBrinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
