package com.loja.checkout.estrategia.clube;

import com.loja.checkout.util.Moeda;

import java.math.BigDecimal;

public class Ouro implements NivelClube {
    private static final BigDecimal PERCENTUAL_CREDITO = new BigDecimal("0.05");
    private static final BigDecimal MINIMO_BRINDE = Moeda.de(500.00);

    @Override
    public BigDecimal calcularCredito(BigDecimal valorProdutos) {
        return Moeda.arredondar(valorProdutos.multiply(PERCENTUAL_CREDITO));
    }

    @Override
    public boolean freteGratis() {
        return true;
    }

    @Override
    public boolean ganhaBrinde(BigDecimal valorProdutos) {
        return valorProdutos.compareTo(MINIMO_BRINDE) > 0;
    }
}
