package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Formas de entrega disponiveis. Cada modalidade define o proprio frete, o
 * proprio prazo e a propria restricao de atendimento.
 */
public enum ModalidadeEntrega {

    ECONOMICA(7) {
        @Override
        public BigDecimal frete(BigDecimal pesoKg) {
            return porKg(new BigDecimal("12.00"), new BigDecimal("2.00"), pesoKg);
        }
    },

    EXPRESSA(2) {
        @Override
        public BigDecimal frete(BigDecimal pesoKg) {
            return porKg(new BigDecimal("25.00"), new BigDecimal("4.50"), pesoKg);
        }
    },

    RETIRADA_LOJA(1) {
        @Override
        public BigDecimal frete(BigDecimal pesoKg) {
            return BigDecimal.ZERO;
        }
    },

    MOTOBOY(0) {
        private static final BigDecimal PESO_MAXIMO_KG = new BigDecimal("5");

        @Override
        public BigDecimal frete(BigDecimal pesoKg) {
            return new BigDecimal("18.00");
        }

        @Override
        public boolean atende(BigDecimal pesoKg) {
            return pesoKg.compareTo(PESO_MAXIMO_KG) <= 0;
        }
    };

    private static final Map<String, ModalidadeEntrega> POR_CODIGO = Stream.of(values())
            .collect(Collectors.toMap(Enum::name, Function.identity()));

    private final int prazoDias;

    ModalidadeEntrega(int prazoDias) {
        this.prazoDias = prazoDias;
    }

    public static ModalidadeEntrega resolver(String codigo) {
        ModalidadeEntrega modalidade = codigo == null ? null : POR_CODIGO.get(codigo);
        if (modalidade == null) {
            throw new ErroCheckout("MODALIDADE_INVALIDA");
        }
        return modalidade;
    }

    public abstract BigDecimal frete(BigDecimal pesoKg);

    public boolean atende(BigDecimal pesoKg) {
        return true;
    }

    public int prazoDias() {
        return prazoDias;
    }

    static BigDecimal porKg(BigDecimal base, BigDecimal porKg, BigDecimal pesoKg) {
        return base.add(porKg.multiply(pesoKg));
    }
}
