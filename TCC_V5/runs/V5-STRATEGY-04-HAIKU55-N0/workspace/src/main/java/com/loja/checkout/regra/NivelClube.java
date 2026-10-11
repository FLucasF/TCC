package com.loja.checkout.regra;

import java.math.BigDecimal;

public enum NivelClube {

    BRONZE("0"),
    PRATA("0.02"),
    OURO("0.05");

    private final BigDecimal percentualCredito;

    NivelClube(String percentualCredito) {
        this.percentualCredito = new BigDecimal(percentualCredito);
    }

    public BigDecimal percentualCredito() {
        return percentualCredito;
    }

    static NivelClube de(String valor) {
        return ErroCheckout.paraEnum(NivelClube.class, valor, CodigoErro.NIVEL_CLUBE_INVALIDO);
    }
}
