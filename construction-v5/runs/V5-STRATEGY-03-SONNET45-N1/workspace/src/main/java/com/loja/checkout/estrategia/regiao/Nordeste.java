package com.loja.checkout.estrategia.regiao;

import com.loja.checkout.util.Moeda;

import java.math.BigDecimal;

public class Nordeste implements Regiao {
    private static final BigDecimal PERCENTUAL = new BigDecimal("0.02");

    @Override
    public BigDecimal calcularSeguro(BigDecimal valorProdutos) {
        return Moeda.arredondar(valorProdutos.multiply(PERCENTUAL));
    }
}
