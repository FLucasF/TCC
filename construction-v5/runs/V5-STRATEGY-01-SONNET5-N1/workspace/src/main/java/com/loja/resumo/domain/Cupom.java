package com.loja.resumo.domain;

import java.math.BigDecimal;

/**
 * Cada cupom tem sua própria condição de uso e sua própria conta de desconto.
 * Promoção nova do marketing = constante nova.
 */
public enum Cupom {

    BEMVINDO10 {
        @Override
        public boolean aplicavel(ContextoCupom contexto) {
            return true;
        }

        @Override
        public BigDecimal calcularDesconto(ContextoCupom contexto) {
            return Dinheiro.arredondar(contexto.subtotalProdutos().multiply(new BigDecimal("0.10")));
        }
    },

    MENOS50 {
        private static final BigDecimal VALOR_MINIMO = new BigDecimal("300.00");
        private static final BigDecimal DESCONTO = new BigDecimal("50.00");

        @Override
        public boolean aplicavel(ContextoCupom contexto) {
            return contexto.subtotalProdutos().compareTo(VALOR_MINIMO) >= 0;
        }

        @Override
        public BigDecimal calcularDesconto(ContextoCupom contexto) {
            return Dinheiro.arredondar(DESCONTO);
        }
    },

    FRETEGRATIS {
        @Override
        public boolean aplicavel(ContextoCupom contexto) {
            return true;
        }

        @Override
        public BigDecimal calcularDesconto(ContextoCupom contexto) {
            return Dinheiro.arredondar(contexto.frete());
        }
    },

    LEVE3PAGUE2 {
        @Override
        public boolean aplicavel(ContextoCupom contexto) {
            return true;
        }

        @Override
        public BigDecimal calcularDesconto(ContextoCupom contexto) {
            BigDecimal desconto = BigDecimal.ZERO;
            for (ItemPedido item : contexto.itens()) {
                int unidadesGratis = item.quantidade() / 3;
                desconto = desconto.add(item.precoUnitario().multiply(BigDecimal.valueOf(unidadesGratis)));
            }
            return Dinheiro.arredondar(desconto);
        }
    };

    public abstract boolean aplicavel(ContextoCupom contexto);

    public abstract BigDecimal calcularDesconto(ContextoCupom contexto);
}
