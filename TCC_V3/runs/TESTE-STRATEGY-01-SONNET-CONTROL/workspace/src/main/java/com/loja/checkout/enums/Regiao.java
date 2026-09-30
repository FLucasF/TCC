package com.loja.checkout.enums;

import java.math.BigDecimal;

/** Alíquota de imposto por região. Só a porcentagem muda entre elas. */
public enum Regiao {

    SUDESTE(new BigDecimal("0.12")),
    SUL(new BigDecimal("0.11")),
    CENTRO_OESTE(new BigDecimal("0.09")),
    NORTE(new BigDecimal("0.07")),
    NORDESTE(new BigDecimal("0.07"));

    private final BigDecimal aliquota;

    Regiao(BigDecimal aliquota) {
        this.aliquota = aliquota;
    }

    public BigDecimal aliquota() {
        return aliquota;
    }
}
