package com.loja.dominio;

import java.math.BigDecimal;

public enum Regiao {
    SUDESTE("0.01"),
    SUL("0.01"),
    CENTRO_OESTE("0.015"),
    NORTE("0.025"),
    NORDESTE("0.02");

    private final BigDecimal percentual;

    Regiao(String p) {
        this.percentual = new BigDecimal(p);
    }

    public BigDecimal percentual() {
        return percentual;
    }

    public static Regiao parse(String s) {
        if (s == null) throw new ErroCheckout("REGIAO_INVALIDA");
        try {
            return Regiao.valueOf(s);
        } catch (IllegalArgumentException e) {
            throw new ErroCheckout("REGIAO_INVALIDA");
        }
    }
}
