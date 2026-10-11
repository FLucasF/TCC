package com.loja.checkout.calculo;

import java.math.BigDecimal;

public enum Cupom {

    BEMVINDO10 {
        BigDecimal desconto(Totais totais) {
            return totais.subtotal().multiply(new BigDecimal("0.10"));
        }
    },

    MENOS50 {
        boolean aplicavel(Totais totais) {
            return totais.subtotal().compareTo(new BigDecimal("300.00")) >= 0;
        }

        BigDecimal desconto(Totais totais) {
            return new BigDecimal("50.00");
        }
    },

    FRETEGRATIS {
        BigDecimal desconto(Totais totais) {
            return totais.frete();
        }
    },

    LEVE3PAGUE2 {
        BigDecimal desconto(Totais totais) {
            return totais.itens().stream()
                    .map(item -> item.precoUnitario().multiply(BigDecimal.valueOf(item.quantidade() / 3)))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
    };

    boolean aplicavel(Totais totais) {
        return true;
    }

    abstract BigDecimal desconto(Totais totais);
}
