package com.loja.resumo;

import java.math.BigDecimal;
import java.util.Arrays;

enum ModalidadeEntrega {
    ECONOMICA(new BigDecimal("12.00"), new BigDecimal("2.00"), 7),
    EXPRESSA(new BigDecimal("25.00"), new BigDecimal("4.50"), 2),
    RETIRADA_LOJA(BigDecimal.ZERO, BigDecimal.ZERO, 1),
    MOTOBOY(new BigDecimal("18.00"), BigDecimal.ZERO, 0) {
        @Override
        void validarDisponibilidade(BigDecimal pesoKg) {
            if (pesoKg.compareTo(PESO_MAXIMO_MOTOBOY) > 0) {
                throw new ErroCompra(CodigoErro.MODALIDADE_INDISPONIVEL);
            }
        }
    };

    static final BigDecimal PESO_MAXIMO_MOTOBOY = new BigDecimal("5");

    private final BigDecimal taxaBase;
    private final BigDecimal taxaPorKg;
    private final int prazoDias;

    ModalidadeEntrega(BigDecimal taxaBase, BigDecimal taxaPorKg, int prazoDias) {
        this.taxaBase = taxaBase;
        this.taxaPorKg = taxaPorKg;
        this.prazoDias = prazoDias;
    }

    BigDecimal frete(BigDecimal pesoKg) {
        return taxaBase.add(taxaPorKg.multiply(pesoKg));
    }

    void validarDisponibilidade(BigDecimal pesoKg) {
    }

    int prazoDias() {
        return prazoDias;
    }

    static ModalidadeEntrega de(String nome) {
        return Arrays.stream(values())
                .filter(m -> m.name().equals(nome))
                .findFirst()
                .orElseThrow(() -> new ErroCompra(CodigoErro.MODALIDADE_INVALIDA));
    }
}
