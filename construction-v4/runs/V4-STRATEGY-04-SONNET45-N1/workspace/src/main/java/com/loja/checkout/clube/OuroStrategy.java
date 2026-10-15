package com.loja.checkout.clube;

import com.loja.checkout.util.Arredondamento;

import java.math.BigDecimal;

public class OuroStrategy implements ClubeStrategy {
    private static final BigDecimal VALOR_MINIMO_BRINDE = new BigDecimal("500.00");

    @Override
    public boolean temFreteGratis() {
        return true;
    }

    @Override
    public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
        BigDecimal credito = subtotalProdutos.multiply(new BigDecimal("0.05"));
        return Arredondamento.arredondar(credito);
    }

    @Override
    public boolean temBrinde(BigDecimal subtotalProdutos) {
        return subtotalProdutos.compareTo(VALOR_MINIMO_BRINDE) > 0;
    }
}
