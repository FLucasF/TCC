package com.loja.checkout.dominio;

import java.math.BigDecimal;

/**
 * Cupons de desconto. Cada promoção tem sua própria condição para valer e seu
 * próprio jeito de descontar, e as duas coisas moram no próprio cupom. Uma
 * promoção nova entra como uma constante nova aqui.
 */
public enum Cupom {

    BEMVINDO10 {
        @Override
        public BigDecimal desconto(ContextoCupom ctx) {
            return Dinheiro.centavos(ctx.subtotalProdutos().multiply(new BigDecimal("0.10")));
        }
    },

    MENOS50 {
        @Override
        public boolean aplicavel(ContextoCupom ctx) {
            return ctx.subtotalProdutos().compareTo(new BigDecimal("300.00")) >= 0;
        }

        @Override
        public BigDecimal desconto(ContextoCupom ctx) {
            return Dinheiro.centavos(new BigDecimal("50.00"));
        }
    },

    FRETEGRATIS {
        @Override
        public BigDecimal desconto(ContextoCupom ctx) {
            return Dinheiro.centavos(ctx.frete());
        }
    },

    LEVE3PAGUE2 {
        @Override
        public BigDecimal desconto(ContextoCupom ctx) {
            BigDecimal gratis = BigDecimal.ZERO;
            for (ItemPedido item : ctx.itens()) {
                int unidadesGratis = item.quantidade() / 3;
                gratis = gratis.add(item.precoUnitario().multiply(BigDecimal.valueOf(unidadesGratis)));
            }
            return Dinheiro.centavos(gratis);
        }
    };

    /** Se o cupom existe mas o pedido não cumpre a condição (ex.: MENOS50 abaixo de R$ 300,00). */
    public boolean aplicavel(ContextoCupom ctx) {
        return true;
    }

    public abstract BigDecimal desconto(ContextoCupom ctx);
}
