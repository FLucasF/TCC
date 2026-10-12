package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.Optional;

public enum Regiao {
    SUDESTE("0.01"),
    SUL("0.01"),
    CENTRO_OESTE("0.015"),
    NORTE("0.025"),
    NORDESTE("0.02");

    private final BigDecimal percentualSeguro;

    Regiao(String percentual) {
        this.percentualSeguro = new BigDecimal(percentual);
    }

    public BigDecimal percentualSeguro() {
        return percentualSeguro;
    }

    public static Optional<Regiao> de(String codigo) {
        if (codigo == null) return Optional.empty();
        try {
            return Optional.of(Regiao.valueOf(codigo));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
