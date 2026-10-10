package com.loja.checkout.dominio;

import java.math.BigDecimal;

/**
 * Opcoes de entrega. Cada parceria tem seu jeito de cobrar, seu prazo e suas
 * limitacoes, por isso cada opcao traz as tres respostas dentro de si: incluir
 * uma transportadora nova e acrescentar uma constante aqui.
 */
public enum ModalidadeEntrega {

    ECONOMICA {
        @Override
        public Entrega cotar(BigDecimal pesoKg) {
            return new Entrega(porKg(new BigDecimal("12.00"), new BigDecimal("2.00"), pesoKg), 7);
        }
    },

    EXPRESSA {
        @Override
        public Entrega cotar(BigDecimal pesoKg) {
            return new Entrega(porKg(new BigDecimal("25.00"), new BigDecimal("4.50"), pesoKg), 2);
        }
    },

    RETIRADA_LOJA {
        @Override
        public Entrega cotar(BigDecimal pesoKg) {
            return new Entrega(Dinheiro.ZERO, 1);
        }
    },

    MOTOBOY {
        private static final BigDecimal PESO_MAXIMO_KG = new BigDecimal("5");

        @Override
        public Entrega cotar(BigDecimal pesoKg) {
            return new Entrega(Dinheiro.arredondar(new BigDecimal("18.00")), 0);
        }

        @Override
        public boolean atende(BigDecimal pesoKg) {
            return pesoKg.compareTo(PESO_MAXIMO_KG) <= 0;
        }
    };

    public abstract Entrega cotar(BigDecimal pesoKg);

    /** Por padrao a modalidade atende qualquer pedido. */
    public boolean atende(BigDecimal pesoKg) {
        return true;
    }

    static BigDecimal porKg(BigDecimal fixo, BigDecimal porKg, BigDecimal pesoKg) {
        return Dinheiro.arredondar(fixo.add(porKg.multiply(pesoKg)));
    }
}
