package com.loja.resumo.calculo;

import java.math.BigDecimal;

import static com.loja.resumo.calculo.Dinheiro.ZERO;
import static com.loja.resumo.calculo.Dinheiro.arredonda;

public enum NivelClube {

    BRONZE(BigDecimal.ZERO),
    PRATA(new BigDecimal("0.02")),
    OURO(new BigDecimal("0.05")) {
        @Override
        BigDecimal aplicarFrete(BigDecimal frete) {
            return ZERO;
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

    BigDecimal creditoSobre(BigDecimal subtotal) {
        return arredonda(subtotal.multiply(percentualCredito));
    }

    BigDecimal aplicarFrete(BigDecimal frete) {
        return frete;
    }

    boolean temBrinde(BigDecimal subtotal) {
        return false;
    }
}
