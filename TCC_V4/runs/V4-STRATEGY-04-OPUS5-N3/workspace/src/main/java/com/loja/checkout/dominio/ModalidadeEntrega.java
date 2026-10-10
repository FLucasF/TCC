package com.loja.checkout.dominio;

import com.loja.checkout.comum.Dinheiro;
import java.math.BigDecimal;

/**
 * Formas de entrega. Cada uma traz o seu custo, o seu prazo e as suas limitações.
 * Entrega nova = constante nova aqui, sem mexer no resto.
 */
public enum ModalidadeEntrega {

    ECONOMICA(7) {
        private static final BigDecimal FIXO = new BigDecimal("12.00");
        private static final BigDecimal POR_KG = new BigDecimal("2.00");

        @Override
        public BigDecimal custo(BigDecimal pesoKg) {
            return fixoMaisPorKg(FIXO, POR_KG, pesoKg);
        }
    },

    EXPRESSA(2) {
        private static final BigDecimal FIXO = new BigDecimal("25.00");
        private static final BigDecimal POR_KG = new BigDecimal("4.50");

        @Override
        public BigDecimal custo(BigDecimal pesoKg) {
            return fixoMaisPorKg(FIXO, POR_KG, pesoKg);
        }
    },

    RETIRADA_LOJA(1) {
        @Override
        public BigDecimal custo(BigDecimal pesoKg) {
            return Dinheiro.ZERO;
        }
    },

    MOTOBOY(0) {
        private static final BigDecimal CUSTO = new BigDecimal("18.00");
        private static final BigDecimal PESO_MAXIMO = new BigDecimal("5");

        @Override
        public BigDecimal custo(BigDecimal pesoKg) {
            return CUSTO;
        }

        @Override
        public boolean atende(BigDecimal pesoKg) {
            return pesoKg.compareTo(PESO_MAXIMO) <= 0;
        }
    };

    private final int prazoDias;

    ModalidadeEntrega(int prazoDias) {
        this.prazoDias = prazoDias;
    }

    public abstract BigDecimal custo(BigDecimal pesoKg);

    /** Se a modalidade existe mas não dá conta deste pedido. */
    public boolean atende(BigDecimal pesoKg) {
        return true;
    }

    public int prazoDias() {
        return prazoDias;
    }

    static BigDecimal fixoMaisPorKg(BigDecimal fixo, BigDecimal porKg, BigDecimal pesoKg) {
        return Dinheiro.centavos(fixo.add(porKg.multiply(pesoKg)));
    }
}
