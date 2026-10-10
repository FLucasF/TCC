package com.loja.checkout.domain;

import com.loja.checkout.web.dto.ItemRequest;

import java.math.BigDecimal;

/**
 * Cupons de desconto vigentes. Cada constante sabe dizer se se aplica a um
 * pedido e calcular o proprio desconto - uma nova promocao do marketing
 * entra como uma nova constante.
 */
public enum Cupom {

    BEMVINDO10 {
        @Override
        public boolean aplicavel(ContextoCalculo ctx) {
            return true;
        }

        @Override
        public BigDecimal calcularDesconto(ContextoCalculo ctx) {
            return Dinheiro.arredondar(ctx.subtotalProdutos().multiply(new BigDecimal("0.10")));
        }
    },

    MENOS50 {
        private static final BigDecimal VALOR_MINIMO = new BigDecimal("300.00");
        private static final BigDecimal DESCONTO = new BigDecimal("50.00");

        @Override
        public boolean aplicavel(ContextoCalculo ctx) {
            return ctx.subtotalProdutos().compareTo(VALOR_MINIMO) >= 0;
        }

        @Override
        public BigDecimal calcularDesconto(ContextoCalculo ctx) {
            return DESCONTO;
        }
    },

    FRETEGRATIS {
        @Override
        public boolean aplicavel(ContextoCalculo ctx) {
            return true;
        }

        @Override
        public BigDecimal calcularDesconto(ContextoCalculo ctx) {
            return ctx.frete();
        }
    },

    LEVE3PAGUE2 {
        @Override
        public boolean aplicavel(ContextoCalculo ctx) {
            return true;
        }

        @Override
        public BigDecimal calcularDesconto(ContextoCalculo ctx) {
            BigDecimal desconto = BigDecimal.ZERO;
            for (ItemRequest item : ctx.itens()) {
                int unidadesGratis = item.quantidade() / 3;
                if (unidadesGratis > 0) {
                    desconto = desconto.add(item.precoUnitario().multiply(BigDecimal.valueOf(unidadesGratis)));
                }
            }
            return Dinheiro.arredondar(desconto);
        }
    };

    public abstract boolean aplicavel(ContextoCalculo ctx);

    public abstract BigDecimal calcularDesconto(ContextoCalculo ctx);
}
