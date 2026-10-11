package com.loja.checkout.domain.clube;

import com.loja.checkout.domain.Arredondamento;
import java.math.BigDecimal;

public class Ouro implements NivelClube {
    private static final BigDecimal PERCENTUAL_CREDITO = new BigDecimal("0.05");
    private static final BigDecimal MINIMO_BRINDE = new BigDecimal("500.00");

    @Override
    public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
        BigDecimal credito = subtotalProdutos.multiply(PERCENTUAL_CREDITO);
        return Arredondamento.arredondar(credito);
    }

    @Override
    public boolean freteGratis() {
        return true;
    }

    @Override
    public boolean ganhaBrinde(BigDecimal subtotalProdutos) {
        return subtotalProdutos.compareTo(MINIMO_BRINDE) > 0;
    }
}
