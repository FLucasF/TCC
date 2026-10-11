package com.loja.checkout.domain;

import java.math.BigDecimal;

/**
 * Cupons de desconto. Cada cupom guarda no seu próprio bloco a regra do desconto
 * e a condição para poder ser usado. Para uma promoção nova, basta mais uma
 * constante aqui.
 */
public enum Cupom {

    /** 10% sobre o valor dos produtos. */
    BEMVINDO10 {
        @Override
        public BigDecimal desconto(ContextoCupom ctx) {
            return ctx.subtotalProdutos().multiply(new BigDecimal("0.10"));
        }
    },
    /** R$ 50,00 de desconto, só a partir de R$ 300,00 em produtos. */
    MENOS50 {
        @Override
        public BigDecimal desconto(ContextoCupom ctx) {
            return new BigDecimal("50.00");
        }

        @Override
        public boolean aplicavel(ContextoCupom ctx) {
            return ctx.subtotalProdutos().compareTo(new BigDecimal("300.00")) >= 0;
        }
    },
    /** Zera o frete: o desconto é igual ao frete que aparece no resumo. */
    FRETEGRATIS {
        @Override
        public BigDecimal desconto(ContextoCupom ctx) {
            return ctx.frete();
        }
    },
    /** A cada 3 unidades de um mesmo item, uma sai de graça. */
    LEVE3PAGUE2 {
        @Override
        public BigDecimal desconto(ContextoCupom ctx) {
            return ctx.itens().stream()
                    .map(item -> item.precoUnitario().multiply(BigDecimal.valueOf(item.unidadesGratis())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
    };

    /** Valor do desconto (ainda sem arredondar). */
    public abstract BigDecimal desconto(ContextoCupom ctx);

    /** Se o cupom pode ser usado neste pedido. Por padrão, pode. */
    public boolean aplicavel(ContextoCupom ctx) {
        return true;
    }
}
