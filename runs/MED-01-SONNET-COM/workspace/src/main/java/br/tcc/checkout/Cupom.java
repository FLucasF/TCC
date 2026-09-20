package br.tcc.checkout;

import java.math.BigDecimal;

enum Cupom {

    BEMVINDO10 {
        @Override
        boolean aplicavelPara(PedidoContexto pedido) {
            return true;
        }

        @Override
        BigDecimal calcularDesconto(PedidoContexto pedido, BigDecimal frete) {
            return pedido.subtotalProdutos().multiply(new BigDecimal("0.10"));
        }
    },

    MENOS50 {
        private final BigDecimal subtotalMinimo = new BigDecimal("300.00");
        private final BigDecimal desconto = new BigDecimal("50.00");

        @Override
        boolean aplicavelPara(PedidoContexto pedido) {
            return pedido.subtotalProdutos().compareTo(subtotalMinimo) >= 0;
        }

        @Override
        BigDecimal calcularDesconto(PedidoContexto pedido, BigDecimal frete) {
            return desconto;
        }
    },

    FRETEGRATIS {
        @Override
        boolean aplicavelPara(PedidoContexto pedido) {
            return true;
        }

        @Override
        BigDecimal calcularDesconto(PedidoContexto pedido, BigDecimal frete) {
            return frete;
        }
    },

    LEVE3PAGUE2 {
        @Override
        boolean aplicavelPara(PedidoContexto pedido) {
            return true;
        }

        @Override
        BigDecimal calcularDesconto(PedidoContexto pedido, BigDecimal frete) {
            BigDecimal desconto = BigDecimal.ZERO;
            for (ItemPedido item : pedido.itens()) {
                int unidadesGratis = item.quantidade() / 3;
                desconto = desconto.add(item.precoUnitario().multiply(BigDecimal.valueOf(unidadesGratis)));
            }
            return desconto;
        }
    };

    abstract boolean aplicavelPara(PedidoContexto pedido);

    abstract BigDecimal calcularDesconto(PedidoContexto pedido, BigDecimal frete);
}
