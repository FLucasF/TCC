package com.loja.checkout.regiao;

import com.loja.checkout.ErroPedido;

import java.math.BigDecimal;

public enum Regiao {
    SUDESTE(new BigDecimal("0.01")),
    SUL(new BigDecimal("0.01")),
    CENTRO_OESTE(new BigDecimal("0.015")),
    NORTE(new BigDecimal("0.025")),
    NORDESTE(new BigDecimal("0.02"));

    public final BigDecimal percentualSeguro;

    Regiao(BigDecimal percentualSeguro) {
        this.percentualSeguro = percentualSeguro;
    }

    public static Regiao obter(String nome) {
        if (nome == null) throw new ErroPedido("REGIAO_INVALIDA");
        try {
            return Regiao.valueOf(nome);
        } catch (IllegalArgumentException e) {
            throw new ErroPedido("REGIAO_INVALIDA");
        }
    }
}
