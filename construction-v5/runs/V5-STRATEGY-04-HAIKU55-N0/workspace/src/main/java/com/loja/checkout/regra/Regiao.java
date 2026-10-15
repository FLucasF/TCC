package com.loja.checkout.regra;

import java.math.BigDecimal;

public enum Regiao {

    SUDESTE("0.01"),
    SUL("0.01"),
    CENTRO_OESTE("0.015"),
    NORTE("0.025"),
    NORDESTE("0.02");

    private final BigDecimal taxaSeguro;

    Regiao(String taxaSeguro) {
        this.taxaSeguro = new BigDecimal(taxaSeguro);
    }

    public BigDecimal taxaSeguro() {
        return taxaSeguro;
    }

    static Regiao de(String valor) {
        return ErroCheckout.paraEnum(Regiao.class, valor, CodigoErro.REGIAO_INVALIDA);
    }
}
