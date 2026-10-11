package com.loja.resumo;

import java.math.BigDecimal;
import java.util.Arrays;

enum Regiao {
    SUDESTE(new BigDecimal("0.01")),
    SUL(new BigDecimal("0.01")),
    CENTRO_OESTE(new BigDecimal("0.015")),
    NORTE(new BigDecimal("0.025")),
    NORDESTE(new BigDecimal("0.02"));

    private final BigDecimal percentualSeguro;

    Regiao(BigDecimal percentualSeguro) {
        this.percentualSeguro = percentualSeguro;
    }

    BigDecimal seguro(BigDecimal subtotal) {
        return Dinheiro.sobre(subtotal, percentualSeguro);
    }

    static Regiao de(String nome) {
        return Arrays.stream(values())
                .filter(r -> r.name().equals(nome))
                .findFirst()
                .orElseThrow(() -> new ErroCompra(CodigoErro.REGIAO_INVALIDA));
    }
}
