package com.loja.checkout.domain;

import com.loja.checkout.dto.ItemPedidoDto;

import java.math.BigDecimal;

/**
 * Cada cupom concentra sua propria condicao de uso e sua propria forma de
 * calcular o desconto.
 */
public enum Cupom {

    BEMVINDO10 {
        @Override
        public boolean aplicavel(ContextoCupom contexto) {
            return true;
        }

        @Override
        public BigDecimal calcularDesconto(ContextoCupom contexto) {
            return contexto.subtotalProdutos().multiply(new BigDecimal("0.10"));
        }
    },

    MENOS50 {
        @Override
        public boolean aplicavel(ContextoCupom contexto) {
            return contexto.subtotalProdutos().compareTo(new BigDecimal("300.00")) >= 0;
        }

        @Override
        public BigDecimal calcularDesconto(ContextoCupom contexto) {
            return new BigDecimal("50.00");
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
            for (ItemPedidoDto item : contexto.itens()) {
                int unidadesGratis = item.quantidade() / 3;
                desconto = desconto.add(item.precoUnitario().multiply(BigDecimal.valueOf(unidadesGratis)));
            }
            return desconto;
        }
    };

    public abstract boolean aplicavel(ContextoCupom contexto);

    public abstract BigDecimal calcularDesconto(ContextoCupom contexto);
}
