package com.loja.checkout.domain.cupom;

import com.loja.checkout.domain.pedido.ItemPedido;

import java.math.BigDecimal;

public enum Cupom {

    BEMVINDO10 {
        @Override
        public BigDecimal calcularDesconto(ContextoDesconto contexto) {
            return contexto.subtotalProdutos().multiply(new BigDecimal("0.10"));
        }
    },

    MENOS50 {
        @Override
        public boolean aplicavel(ContextoDesconto contexto) {
            return contexto.subtotalProdutos().compareTo(new BigDecimal("300")) >= 0;
        }

        @Override
        public BigDecimal calcularDesconto(ContextoDesconto contexto) {
            return new BigDecimal("50.00");
        }
    },

    FRETEGRATIS {
        @Override
        public BigDecimal calcularDesconto(ContextoDesconto contexto) {
            return contexto.frete();
        }
    },

    LEVE3PAGUE2 {
        @Override
        public BigDecimal calcularDesconto(ContextoDesconto contexto) {
            BigDecimal desconto = BigDecimal.ZERO;
            for (ItemPedido item : contexto.itens()) {
                int unidadesGratis = item.quantidade() / 3;
                desconto = desconto.add(item.precoUnitario().multiply(BigDecimal.valueOf(unidadesGratis)));
            }
            return desconto;
        }
    };

    public boolean aplicavel(ContextoDesconto contexto) {
        return true;
    }

    public abstract BigDecimal calcularDesconto(ContextoDesconto contexto);
}
