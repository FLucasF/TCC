package com.loja.checkout.domain.cupom;

import com.loja.checkout.domain.Dinheiro;
import com.loja.checkout.domain.Item;

import java.math.BigDecimal;

/**
 * Cada cupom decide sua própria condição de aplicabilidade e sua própria
 * fórmula de desconto.
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
        private static final BigDecimal VALOR_MINIMO_PRODUTOS = new BigDecimal("300.00");
        private static final BigDecimal VALOR_DESCONTO = new BigDecimal("50.00");

        @Override
        public boolean aplicavel(ContextoCupom contexto) {
            return contexto.subtotalProdutos().compareTo(VALOR_MINIMO_PRODUTOS) >= 0;
        }

        @Override
        public BigDecimal calcularDesconto(ContextoCupom contexto) {
            return Dinheiro.arredondar(VALOR_DESCONTO);
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
            for (Item item : contexto.itens()) {
                int unidadesGratis = item.quantidade() / 3;
                desconto = desconto.add(item.precoUnitario().multiply(BigDecimal.valueOf(unidadesGratis)));
            }
            return Dinheiro.arredondar(desconto);
        }
    };

    public abstract boolean aplicavel(ContextoCupom contexto);

    public abstract BigDecimal calcularDesconto(ContextoCupom contexto);
}
