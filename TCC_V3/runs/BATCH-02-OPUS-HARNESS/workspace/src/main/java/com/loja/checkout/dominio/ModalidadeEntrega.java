package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Optional;

/**
 * Opcoes de entrega. Cada opcao define seu proprio frete, prazo e limitacoes;
 * para criar uma nova parceria basta acrescentar uma constante aqui.
 */
public enum ModalidadeEntrega {

    ECONOMICA(7) {
        @Override
        public BigDecimal frete(BigDecimal pesoKg) {
            return porKg(Dinheiro.reais("12.00"), Dinheiro.reais("2.00"), pesoKg);
        }
    },

    EXPRESSA(2) {
        @Override
        public BigDecimal frete(BigDecimal pesoKg) {
            return porKg(Dinheiro.reais("25.00"), Dinheiro.reais("4.50"), pesoKg);
        }
    },

    RETIRADA_LOJA(1) {
        @Override
        public BigDecimal frete(BigDecimal pesoKg) {
            return Dinheiro.ZERO;
        }
    },

    MOTOBOY(0) {
        private static final BigDecimal PESO_MAXIMO_KG = new BigDecimal("5");

        @Override
        public BigDecimal frete(BigDecimal pesoKg) {
            return Dinheiro.centavos(Dinheiro.reais("18.00"));
        }

        @Override
        public boolean atende(BigDecimal pesoKg) {
            return pesoKg.compareTo(PESO_MAXIMO_KG) <= 0;
        }
    };

    private final int prazoDias;

    ModalidadeEntrega(int prazoDias) {
        this.prazoDias = prazoDias;
    }

    public static Optional<ModalidadeEntrega> porCodigo(String codigo) {
        return Arrays.stream(values()).filter(modalidade -> modalidade.name().equals(codigo)).findFirst();
    }

    public abstract BigDecimal frete(BigDecimal pesoKg);

    public boolean atende(BigDecimal pesoKg) {
        return true;
    }

    public int prazoDias() {
        return prazoDias;
    }

    static BigDecimal porKg(BigDecimal fixo, BigDecimal porKg, BigDecimal pesoKg) {
        return Dinheiro.centavos(fixo.add(porKg.multiply(pesoKg)));
    }
}
