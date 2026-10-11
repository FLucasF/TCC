package com.loja.resumo.cupom;

import com.loja.resumo.dinheiro.Dinheiro;
import com.loja.resumo.dominio.Item;
import com.loja.resumo.dominio.Pedido;

import java.math.BigDecimal;

public enum CupomPadrao implements Cupom {

    BEMVINDO10 {
        @Override
        public boolean aplicavel(Pedido pedido, BigDecimal freteExibido) {
            return true;
        }

        @Override
        public BigDecimal desconto(Pedido pedido, BigDecimal freteExibido) {
            return Dinheiro.arredondar(pedido.subtotalProdutos().multiply(new BigDecimal("0.10")));
        }
    },

    MENOS50 {
        @Override
        public boolean aplicavel(Pedido pedido, BigDecimal freteExibido) {
            return pedido.subtotalProdutos().compareTo(MINIMO_PRODUTOS_MENOS50) >= 0;
        }

        @Override
        public BigDecimal desconto(Pedido pedido, BigDecimal freteExibido) {
            return new BigDecimal("50.00");
        }
    },

    FRETEGRATIS {
        @Override
        public boolean aplicavel(Pedido pedido, BigDecimal freteExibido) {
            return true;
        }

        @Override
        public BigDecimal desconto(Pedido pedido, BigDecimal freteExibido) {
            return freteExibido;
        }
    },

    LEVE3PAGUE2 {
        @Override
        public boolean aplicavel(Pedido pedido, BigDecimal freteExibido) {
            return true;
        }

        @Override
        public BigDecimal desconto(Pedido pedido, BigDecimal freteExibido) {
            BigDecimal soma = BigDecimal.ZERO;
            for (Item item : pedido.itens()) {
                int unidadesGratis = item.quantidade() / TAMANHO_GRUPO_LEVE3PAGUE2;
                soma = soma.add(item.precoUnitario().multiply(BigDecimal.valueOf(unidadesGratis)));
            }
            return Dinheiro.arredondar(soma);
        }
    };

    private static final BigDecimal MINIMO_PRODUTOS_MENOS50 = new BigDecimal("300.00");
    private static final int TAMANHO_GRUPO_LEVE3PAGUE2 = 3;
}
