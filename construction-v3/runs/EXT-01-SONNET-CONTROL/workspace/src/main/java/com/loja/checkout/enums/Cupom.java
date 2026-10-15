package com.loja.checkout.enums;

import com.loja.checkout.dto.ItemPedidoRequest;
import com.loja.checkout.service.Dinheiro;

import java.math.BigDecimal;
import java.util.List;

/**
 * Cupons de marketing. Cada um tem sua propria condicao de aplicabilidade e
 * sua propria forma de calcular o desconto.
 */
public enum Cupom {

    BEMVINDO10 {
        @Override
        public boolean aplicavel(List<ItemPedidoRequest> itens, BigDecimal subtotalProdutos) {
            return true;
        }

        @Override
        public BigDecimal calcularDesconto(List<ItemPedidoRequest> itens, BigDecimal subtotalProdutos, BigDecimal frete) {
            return Dinheiro.arredondar(subtotalProdutos.multiply(new BigDecimal("0.10")));
        }
    },

    MENOS50 {
        private static final BigDecimal VALOR_MINIMO = new BigDecimal("300.00");
        private static final BigDecimal VALOR_DESCONTO = new BigDecimal("50.00");

        @Override
        public boolean aplicavel(List<ItemPedidoRequest> itens, BigDecimal subtotalProdutos) {
            return subtotalProdutos.compareTo(VALOR_MINIMO) >= 0;
        }

        @Override
        public BigDecimal calcularDesconto(List<ItemPedidoRequest> itens, BigDecimal subtotalProdutos, BigDecimal frete) {
            return VALOR_DESCONTO;
        }
    },

    FRETEGRATIS {
        @Override
        public boolean aplicavel(List<ItemPedidoRequest> itens, BigDecimal subtotalProdutos) {
            return true;
        }

        @Override
        public BigDecimal calcularDesconto(List<ItemPedidoRequest> itens, BigDecimal subtotalProdutos, BigDecimal frete) {
            return frete;
        }
    },

    LEVE3PAGUE2 {
        @Override
        public boolean aplicavel(List<ItemPedidoRequest> itens, BigDecimal subtotalProdutos) {
            return true;
        }

        @Override
        public BigDecimal calcularDesconto(List<ItemPedidoRequest> itens, BigDecimal subtotalProdutos, BigDecimal frete) {
            BigDecimal desconto = BigDecimal.ZERO;
            for (ItemPedidoRequest item : itens) {
                int unidadesGratis = item.quantidade() / 3;
                desconto = desconto.add(item.precoUnitario().multiply(BigDecimal.valueOf(unidadesGratis)));
            }
            return Dinheiro.arredondar(desconto);
        }
    };

    public abstract boolean aplicavel(List<ItemPedidoRequest> itens, BigDecimal subtotalProdutos);

    public abstract BigDecimal calcularDesconto(List<ItemPedidoRequest> itens, BigDecimal subtotalProdutos, BigDecimal frete);
}
