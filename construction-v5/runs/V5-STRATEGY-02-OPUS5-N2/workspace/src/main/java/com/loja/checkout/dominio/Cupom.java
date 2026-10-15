package com.loja.checkout.dominio;

import java.math.BigDecimal;

/**
 * Cada cupom define a sua condição e a sua conta de desconto.
 * Promoção nova entra como uma constante nova.
 */
public enum Cupom {

    BEMVINDO10 {
        @Override
        protected BigDecimal calcular(ContextoCupom contexto) {
            return Dinheiro.percentual(contexto.subtotalProdutos(), Dinheiro.taxa("0.10"));
        }
    },

    MENOS50 {
        @Override
        protected boolean aplicavel(ContextoCupom contexto) {
            return contexto.subtotalProdutos().compareTo(new BigDecimal("300.00")) >= 0;
        }

        @Override
        protected BigDecimal calcular(ContextoCupom contexto) {
            return Dinheiro.arredondar(new BigDecimal("50.00"));
        }
    },

    FRETEGRATIS {
        @Override
        protected BigDecimal calcular(ContextoCupom contexto) {
            return Dinheiro.arredondar(contexto.frete());
        }
    },

    LEVE3PAGUE2 {
        @Override
        protected BigDecimal calcular(ContextoCupom contexto) {
            return Dinheiro.arredondar(contexto.itens().stream()
                    .map(item -> item.precoUnitario().multiply(BigDecimal.valueOf(item.quantidade() / 3)))
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
        }
    };

    public final BigDecimal desconto(ContextoCupom contexto) {
        if (!aplicavel(contexto)) {
            throw new PedidoRecusado(CodigoErro.CUPOM_NAO_APLICAVEL);
        }
        return calcular(contexto);
    }

    protected abstract BigDecimal calcular(ContextoCupom contexto);

    protected boolean aplicavel(ContextoCupom contexto) {
        return true;
    }
}
