package com.loja.checkout.clube;

import com.loja.checkout.util.Arredondamento;

import java.math.BigDecimal;

public class PrataStrategy implements ClubeStrategy {
    @Override
    public boolean temFreteGratis() {
        return false;
    }

    @Override
    public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
        BigDecimal credito = subtotalProdutos.multiply(new BigDecimal("0.02"));
        return Arredondamento.arredondar(credito);
    }

    @Override
    public boolean temBrinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
