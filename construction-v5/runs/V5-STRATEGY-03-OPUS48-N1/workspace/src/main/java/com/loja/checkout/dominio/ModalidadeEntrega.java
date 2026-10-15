package com.loja.checkout.dominio;

import java.math.BigDecimal;

/**
 * Formas de entrega. Cada opcao tem seu jeito de cobrar, seu prazo e seus
 * limites; por isso cada caso mora no proprio corpo do enum. Entregadora nova
 * entra como uma nova constante, sem mexer nas outras nem em quem escolhe.
 */
public enum ModalidadeEntrega {

    ECONOMICA(7) {
        @Override
        public BigDecimal custo(BigDecimal pesoKg) {
            return new BigDecimal("12.00").add(new BigDecimal("2.00").multiply(pesoKg));
        }
    },

    EXPRESSA(2) {
        @Override
        public BigDecimal custo(BigDecimal pesoKg) {
            return new BigDecimal("25.00").add(new BigDecimal("4.50").multiply(pesoKg));
        }
    },

    RETIRADA_LOJA(1) {
        @Override
        public BigDecimal custo(BigDecimal pesoKg) {
            return BigDecimal.ZERO;
        }
    },

    MOTOBOY(0) {
        @Override
        public BigDecimal custo(BigDecimal pesoKg) {
            return new BigDecimal("18.00");
        }

        @Override
        public boolean atende(BigDecimal pesoKg) {
            return pesoKg.compareTo(new BigDecimal("5")) <= 0;
        }
    };

    private final int prazoDias;

    ModalidadeEntrega(int prazoDias) {
        this.prazoDias = prazoDias;
    }

    public int prazoDias() {
        return prazoDias;
    }

    /** Custo bruto do frete; quem chama arredonda para centavos. */
    public abstract BigDecimal custo(BigDecimal pesoKg);

    /** Se a opcao atende um pedido com esse peso. Por padrao, atende. */
    public boolean atende(BigDecimal pesoKg) {
        return true;
    }
}
