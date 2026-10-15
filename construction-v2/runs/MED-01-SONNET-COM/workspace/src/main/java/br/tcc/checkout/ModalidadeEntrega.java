package br.tcc.checkout;

import java.math.BigDecimal;

enum ModalidadeEntrega {

    ECONOMICA {
        @Override
        BigDecimal calcularFrete(PedidoContexto pedido) {
            return new BigDecimal("12.00").add(new BigDecimal("2.00").multiply(pedido.pesoTotalKg()));
        }

        @Override
        int prazoDias() {
            return 7;
        }

        @Override
        boolean disponivelPara(PedidoContexto pedido) {
            return true;
        }
    },

    EXPRESSA {
        @Override
        BigDecimal calcularFrete(PedidoContexto pedido) {
            return new BigDecimal("25.00").add(new BigDecimal("4.50").multiply(pedido.pesoTotalKg()));
        }

        @Override
        int prazoDias() {
            return 2;
        }

        @Override
        boolean disponivelPara(PedidoContexto pedido) {
            return true;
        }
    },

    RETIRADA_LOJA {
        @Override
        BigDecimal calcularFrete(PedidoContexto pedido) {
            return BigDecimal.ZERO;
        }

        @Override
        int prazoDias() {
            return 1;
        }

        @Override
        boolean disponivelPara(PedidoContexto pedido) {
            return true;
        }
    },

    MOTOBOY {
        private final BigDecimal pesoMaximoKg = new BigDecimal("5.00");

        @Override
        BigDecimal calcularFrete(PedidoContexto pedido) {
            return new BigDecimal("18.00");
        }

        @Override
        int prazoDias() {
            return 0;
        }

        @Override
        boolean disponivelPara(PedidoContexto pedido) {
            return pedido.pesoTotalKg().compareTo(pesoMaximoKg) <= 0;
        }
    };

    abstract BigDecimal calcularFrete(PedidoContexto pedido);

    abstract int prazoDias();

    abstract boolean disponivelPara(PedidoContexto pedido);
}
