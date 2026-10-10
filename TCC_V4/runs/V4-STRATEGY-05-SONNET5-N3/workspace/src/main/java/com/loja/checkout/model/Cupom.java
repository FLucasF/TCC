package com.loja.checkout.model;

import com.loja.checkout.dto.ItemRequest;

import java.math.BigDecimal;

public enum Cupom {

    BEMVINDO10 {
        @Override
        public boolean aplicavel(ContextoCupom contexto) {
            return true;
        }

        @Override
        public BigDecimal calcularDesconto(ContextoCupom contexto) {
            return Dinheiro.arredondar(new BigDecimal("0.10").multiply(contexto.subtotalProdutos()));
        }
    },

    MENOS50 {
        @Override
        public boolean aplicavel(ContextoCupom contexto) {
            return contexto.subtotalProdutos().compareTo(MENOS50_SUBTOTAL_MINIMO) >= 0;
        }

        @Override
        public BigDecimal calcularDesconto(ContextoCupom contexto) {
            return MENOS50_DESCONTO;
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
                desconto = desconto.add(item.precoUnitario().multiply(BigDecimal.valueOf(unidadesGratis)));
            }
            return Dinheiro.arredondar(desconto);
        }
    };

    private static final BigDecimal MENOS50_SUBTOTAL_MINIMO = new BigDecimal("300.00");
    private static final BigDecimal MENOS50_DESCONTO = new BigDecimal("50.00");

    public abstract boolean aplicavel(ContextoCupom contexto);

    public abstract BigDecimal calcularDesconto(ContextoCupom contexto);
}
