package com.loja.checkout.dominio;

import java.math.BigDecimal;

public enum Regiao {

    SUDESTE(new BigDecimal("0.12")),
    SUL(new BigDecimal("0.11")),
    CENTRO_OESTE(new BigDecimal("0.09")),
    NORTE(new BigDecimal("0.07")),
    NORDESTE(new BigDecimal("0.07"));

    private final BigDecimal percentualImposto;

    Regiao(BigDecimal percentualImposto) {
        this.percentualImposto = percentualImposto;
    }

    public BigDecimal imposto(BigDecimal baseComDesconto) {
        return Dinheiro.arredondar(baseComDesconto.multiply(percentualImposto));
    }
}
