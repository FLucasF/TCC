package com.loja.checkout.dominio;

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

    public BigDecimal seguro(BigDecimal subtotalProdutos) {
        return Dinheiro.arredondar(subtotalProdutos.multiply(percentual));
    }

    public static Regiao resolver(String codigo) {
        if (codigo == null) throw new ErroPedido("REGIAO_INVALIDA");
        try {
            return valueOf(codigo);
        } catch (IllegalArgumentException e) {
            throw new ErroPedido("REGIAO_INVALIDA");
        }
    }
}
