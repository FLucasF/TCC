package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.Optional;

public enum ModalidadeEntrega implements RegraEntrega {

    ECONOMICA {
        @Override
        public BigDecimal frete(Pedido pedido) {
            return porPeso(pedido, "12.00", "2.00");
        }

        @Override
        public int prazoDias() {
            return 7;
        }
    },

    EXPRESSA {
        @Override
        public BigDecimal frete(Pedido pedido) {
            return porPeso(pedido, "25.00", "4.50");
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
        private static final BigDecimal PESO_MAXIMO_KG = new BigDecimal("5");

        @Override
        public BigDecimal frete(Pedido pedido) {
            return Dinheiro.centavos(new BigDecimal("18.00"));
        }

        @Override
        public int prazoDias() {
            return 0;
        }

        @Override
        public boolean atende(Pedido pedido) {
            return pedido.pesoKg().compareTo(PESO_MAXIMO_KG) <= 0;
        }
    };

    private static final Catalogo<ModalidadeEntrega> CATALOGO = Catalogo.de(ModalidadeEntrega.class);

    public static Optional<ModalidadeEntrega> de(String codigo) {
        return CATALOGO.buscar(codigo);
    }

    static BigDecimal porPeso(Pedido pedido, String fixo, String porKg) {
        return Dinheiro.centavos(new BigDecimal(fixo)
                .add(new BigDecimal(porKg).multiply(pedido.pesoKg())));
    }
}
