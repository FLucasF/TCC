package com.loja.resumo;

import java.math.BigDecimal;
import java.util.Arrays;

enum NivelClube {
    BRONZE(BigDecimal.ZERO),
    PRATA(new BigDecimal("0.02")),
    OURO(new BigDecimal("0.05")) {
        @Override
        BigDecimal ajustarFrete(BigDecimal frete) {
            return BigDecimal.ZERO;
        }

        @Override
        boolean temBrinde(BigDecimal subtotal) {
            return subtotal.compareTo(LIMITE_BRINDE) > 0;
        }
    };

    static final BigDecimal LIMITE_BRINDE = new BigDecimal("500");

    private final BigDecimal percentualCredito;

    NivelClube(BigDecimal percentualCredito) {
        this.percentualCredito = percentualCredito;
    }

    BigDecimal ajustarFrete(BigDecimal frete) {
        return frete;
    }

    BigDecimal credito(BigDecimal subtotal) {
        return Dinheiro.sobre(subtotal, percentualCredito);
    }

    boolean temBrinde(BigDecimal subtotal) {
        return false;
    }

    static NivelClube de(String nome) {
        return Arrays.stream(values())
                .filter(n -> n.name().equals(nome))
                .findFirst()
                .orElseThrow(() -> new ErroCompra(CodigoErro.NIVEL_CLUBE_INVALIDO));
    }
}
