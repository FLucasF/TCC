package com.loja.checkout.enums;

import com.loja.checkout.dto.ItemRequest;
import com.loja.checkout.util.Dinheiro;

import java.math.BigDecimal;

/**
 * Cada cupom define sua própria condição de aplicação e sua própria conta de desconto.
 * Novas promoções entram como uma nova constante do enum.
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
        private static final BigDecimal VALOR_DESCONTO = new BigDecimal("50.00");
        private static final BigDecimal COMPRA_MINIMA = new BigDecimal("300.00");

        @Override
        public boolean aplicavel(ContextoCupom contexto) {
            return contexto.subtotalProdutos().compareTo(COMPRA_MINIMA) >= 0;
        }

        @Override
        public BigDecimal calcularDesconto(ContextoCupom contexto) {
            return VALOR_DESCONTO;
        }
    },

    FRETEGRATIS {
        @Override
        public boolean aplicavel(ContextoCupom contexto) {
            return true;
        }

        @Override
        public BigDecimal calcularDesconto(ContextoCupom contexto) {
            return contexto.frete();
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
            for (ItemRequest item : contexto.itens()) {
                int unidadesGratis = item.quantidade() / 3;
                if (unidadesGratis > 0) {
                    desconto = desconto.add(item.precoUnitario().multiply(BigDecimal.valueOf(unidadesGratis)));
                }
            }
            return Dinheiro.arredondar(desconto);
        }
    };

    public abstract boolean aplicavel(ContextoCupom contexto);

    public abstract BigDecimal calcularDesconto(ContextoCupom contexto);
}
