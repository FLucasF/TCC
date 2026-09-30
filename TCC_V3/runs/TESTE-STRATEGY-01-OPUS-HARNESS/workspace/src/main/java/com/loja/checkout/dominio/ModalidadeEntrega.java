package com.loja.checkout.dominio;

import java.math.BigDecimal;

/**
 * Cada modalidade guarda no seu proprio corpo quanto cobra, em quantos dias
 * entrega e quais pedidos aceita.
 */
public enum ModalidadeEntrega {

    ECONOMICA(7) {
        @Override
        protected BigDecimal custo(BigDecimal pesoKg) {
            return fixoMaisPorKg("12.00", "2.00", pesoKg);
        }
    },

    EXPRESSA(2) {
        @Override
        protected BigDecimal custo(BigDecimal pesoKg) {
            return fixoMaisPorKg("25.00", "4.50", pesoKg);
        }
    },

    RETIRADA_LOJA(1) {
        @Override
        protected BigDecimal custo(BigDecimal pesoKg) {
            return BigDecimal.ZERO;
        }
    },

    MOTOBOY(0) {
        private static final BigDecimal PESO_MAXIMO = new BigDecimal("5");

        @Override
        protected BigDecimal custo(BigDecimal pesoKg) {
            return new BigDecimal("18.00");
        }

        @Override
        public boolean atende(Carrinho carrinho) {
            return carrinho.pesoKg().compareTo(PESO_MAXIMO) <= 0;
        }
    };

    private final int prazoDias;

    ModalidadeEntrega(int prazoDias) {
        this.prazoDias = prazoDias;
    }

    protected abstract BigDecimal custo(BigDecimal pesoKg);

    public boolean atende(Carrinho carrinho) {
        return true;
    }

    public Frete cotar(Carrinho carrinho) {
        return new Frete(Dinheiro.centavos(custo(carrinho.pesoKg())), prazoDias);
    }

    protected static BigDecimal fixoMaisPorKg(String fixo, String porKg, BigDecimal pesoKg) {
        return new BigDecimal(fixo).add(new BigDecimal(porKg).multiply(pesoKg));
    }
}
