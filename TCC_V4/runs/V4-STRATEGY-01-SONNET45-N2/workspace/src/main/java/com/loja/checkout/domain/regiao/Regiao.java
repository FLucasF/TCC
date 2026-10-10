package com.loja.checkout.domain.regiao;

import java.math.BigDecimal;

public enum Regiao {
    SUDESTE(new BigDecimal("0.01")),
    SUL(new BigDecimal("0.01")),
    CENTRO_OESTE(new BigDecimal("0.015")),
    NORTE(new BigDecimal("0.025")),
    NORDESTE(new BigDecimal("0.02"));

    private final BigDecimal porcentagemSeguro;

    Regiao(BigDecimal porcentagemSeguro) {
        this.porcentagemSeguro = porcentagemSeguro;
    }

    public BigDecimal getPorcentagemSeguro() {
        return porcentagemSeguro;
    }
}
