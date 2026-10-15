package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.List;

public enum ModalidadeEntrega {

    ECONOMICA {
        @Override
        public BigDecimal frete(List<ItemPedido> itens, BigDecimal pesoTotalKg) {
            return new BigDecimal("12.00").add(new BigDecimal("2.00").multiply(pesoTotalKg));
        }

        @Override
        public int prazoDias() {
            return 7;
        }
    },

    EXPRESSA {
        @Override
        public BigDecimal frete(List<ItemPedido> itens, BigDecimal pesoTotalKg) {
            return new BigDecimal("25.00").add(new BigDecimal("4.50").multiply(pesoTotalKg));
        }

        @Override
        public int prazoDias() {
            return 2;
        }
    },

    RETIRADA_LOJA {
        @Override
        public BigDecimal frete(List<ItemPedido> itens, BigDecimal pesoTotalKg) {
            return BigDecimal.ZERO;
        }

        @Override
        public int prazoDias() {
            return 1;
        }
    },

    MOTOBOY {
        @Override
        public BigDecimal frete(List<ItemPedido> itens, BigDecimal pesoTotalKg) {
            return new BigDecimal("18.00");
        }

        @Override
        public int prazoDias() {
            return 0;
        }

        @Override
        public boolean disponivel(BigDecimal pesoTotalKg) {
            return pesoTotalKg.compareTo(new BigDecimal("5")) <= 0;
        }
    };

    public abstract BigDecimal frete(List<ItemPedido> itens, BigDecimal pesoTotalKg);

    public abstract int prazoDias();

    public boolean disponivel(BigDecimal pesoTotalKg) {
        return true;
    }
}
