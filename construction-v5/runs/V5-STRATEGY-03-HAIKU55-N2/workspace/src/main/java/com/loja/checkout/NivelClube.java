package com.loja.checkout;

import java.math.BigDecimal;

public enum NivelClube {

    BRONZE(new BigDecimal("0")),
    PRATA(new BigDecimal("0.02")),
    OURO(new BigDecimal("0.05")) {
        @Override
        boolean isentaFrete() {
            return true;
        }

        @Override
        boolean temBrinde(BigDecimal subtotal) {
            return subtotal.compareTo(new BigDecimal("500")) > 0;
        }
    };

    private final BigDecimal percentualCredito;

    NivelClube(BigDecimal percentualCredito) {
        this.percentualCredito = percentualCredito;
    }

    BigDecimal percentualCredito() {
        return percentualCredito;
    }

    boolean isentaFrete() {
        return false;
    }

    boolean temBrinde(BigDecimal subtotal) {
        return false;
    }

    static NivelClube de(String codigo) {
        return ResumoException.paraEnum(values(), codigo, "NIVEL_CLUBE_INVALIDO");
    }
}
