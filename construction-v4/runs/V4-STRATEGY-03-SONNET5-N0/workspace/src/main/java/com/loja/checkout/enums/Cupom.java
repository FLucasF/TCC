package com.loja.checkout.enums;

import com.loja.checkout.dto.ContextoCupom;
import com.loja.checkout.dto.ItemPedido;
import com.loja.checkout.exception.PedidoException;

import java.math.BigDecimal;

/**
 * Cupons fixos de hoje. Cada constante sabe validar sua própria condição e calcular seu desconto.
 */
public enum Cupom {

    BEMVINDO10 {
        @Override
        public void validar(ContextoCupom contexto) {
            // sem condição além de existir
        }

        @Override
        public BigDecimal desconto(ContextoCupom contexto) {
            return contexto.subtotalProdutos().multiply(new BigDecimal("0.10"));
        }
    },
    MENOS50 {
        private static final BigDecimal MINIMO = new BigDecimal("300");

        @Override
        public void validar(ContextoCupom contexto) {
            if (contexto.subtotalProdutos().compareTo(MINIMO) < 0) {
                throw new PedidoException("CUPOM_NAO_APLICAVEL");
            }
        }

        @Override
        public BigDecimal desconto(ContextoCupom contexto) {
            return new BigDecimal("50.00");
        }
    },
    FRETEGRATIS {
        @Override
        public void validar(ContextoCupom contexto) {
            // sem condição além de existir
        }

        @Override
        public BigDecimal desconto(ContextoCupom contexto) {
            return contexto.freteCalculado();
        }
    },
    LEVE3PAGUE2 {
        @Override
        public void validar(ContextoCupom contexto) {
            // sem condição além de existir
        }

        @Override
        public BigDecimal desconto(ContextoCupom contexto) {
            BigDecimal total = BigDecimal.ZERO;
            for (ItemPedido item : contexto.itens()) {
                int unidadesGratis = item.quantidade() / 3;
                if (unidadesGratis > 0) {
                    total = total.add(item.precoUnitario().multiply(BigDecimal.valueOf(unidadesGratis)));
                }
            }
            return total;
        }
    };

    public abstract void validar(ContextoCupom contexto);

    public abstract BigDecimal desconto(ContextoCupom contexto);
}
