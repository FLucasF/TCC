package com.loja.checkout;

import java.math.BigDecimal;

public enum Regiao {

    SUDESTE("0.01"),
    SUL("0.01"),
    CENTRO_OESTE("0.015"),
    NORTE("0.025"),
    NORDESTE("0.02");

    private final BigDecimal percentualSeguro;

    Regiao(String percentualSeguro) {
        this.percentualSeguro = new BigDecimal(percentualSeguro);
    }

    BigDecimal percentualSeguro() {
        return percentualSeguro;
    }

    static Regiao de(String codigo) {
        return ResumoException.paraEnum(values(), codigo, "REGIAO_INVALIDA");
    }
}
