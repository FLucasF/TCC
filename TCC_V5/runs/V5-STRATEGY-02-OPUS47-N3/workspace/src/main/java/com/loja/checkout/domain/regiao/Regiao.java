package com.loja.checkout.domain.regiao;

import java.math.BigDecimal;

public enum Regiao {
    SUDESTE(new BigDecimal("0.010")),
    SUL(new BigDecimal("0.010")),
    CENTRO_OESTE(new BigDecimal("0.015")),
    NORTE(new BigDecimal("0.025")),
    NORDESTE(new BigDecimal("0.020"));

    private final BigDecimal aliquotaSeguro;

    Regiao(BigDecimal aliquotaSeguro) {
        this.aliquotaSeguro = aliquotaSeguro;
    }

    public BigDecimal aliquotaSeguro() {
        return aliquotaSeguro;
    }
}
