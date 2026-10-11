package com.loja.checkout.dominio;

import java.math.BigDecimal;

/**
 * Opcoes de entrega. Cada opcao traz o proprio jeito de cobrar, o proprio
 * prazo e a propria limitacao; entra opcao nova sem mexer nas outras.
 */
public enum ModalidadeEntrega {

    ECONOMICA {
        @Override
        public BigDecimal frete(Pedido pedido) {
            return porKg("12.00", "2.00", pedido);
        }

        @Override
        public int prazoDias() {
            return 7;
        }
    },

    EXPRESSA {
        @Override
        public BigDecimal frete(Pedido pedido) {
            return porKg("25.00", "4.50", pedido);
        }

        @Override
        public int prazoDias() {
            return 2;
        }
    },

    RETIRADA_LOJA {
        @Override
        public BigDecimal frete(Pedido pedido) {
            return Dinheiro.ZERO;
        }

        @Override
        public int prazoDias() {
            return 1;
        }
    },

    MOTOBOY {
        @Override
        public BigDecimal frete(Pedido pedido) {
            return Dinheiro.arredondar(new BigDecimal("18.00"));
        }

        @Override
        public int prazoDias() {
            return 0;
        }

        @Override
        public boolean atende(Pedido pedido) {
            return pedido.pesoKg().compareTo(new BigDecimal("5")) <= 0;
        }
    };

    /** Frete da opcao para este pedido, em centavos. */
    public abstract BigDecimal frete(Pedido pedido);

    public abstract int prazoDias();

    /** Por padrao a opcao atende qualquer pedido. */
    public boolean atende(Pedido pedido) {
        return true;
    }

    static BigDecimal porKg(String fixo, String porKg, Pedido pedido) {
        return Dinheiro.arredondar(new BigDecimal(fixo).add(new BigDecimal(porKg).multiply(pedido.pesoKg())));
    }
}
