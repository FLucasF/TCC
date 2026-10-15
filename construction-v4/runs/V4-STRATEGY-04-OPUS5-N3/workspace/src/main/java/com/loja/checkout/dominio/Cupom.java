package com.loja.checkout.dominio;

import com.loja.checkout.comum.Dinheiro;
import java.math.BigDecimal;

/**
 * Promoções que valem hoje. Cada cupom tem a sua conta e a sua condição de uso.
 * Promoção nova = constante nova aqui.
 */
public enum Cupom {

    BEMVINDO10 {
        private static final BigDecimal TAXA = new BigDecimal("0.10");

        @Override
        public BigDecimal desconto(ContextoCupom contexto) {
            return Dinheiro.percentual(contexto.subtotalProdutos(), TAXA);
        }
    },

    MENOS50 {
        private static final BigDecimal VALOR = new BigDecimal("50.00");
        private static final BigDecimal MINIMO = new BigDecimal("300.00");

        @Override
        public boolean aplicavel(ContextoCupom contexto) {
            return contexto.subtotalProdutos().compareTo(MINIMO) >= 0;
        }

        @Override
        public BigDecimal desconto(ContextoCupom contexto) {
            return VALOR;
        }
    },

    FRETEGRATIS {
        @Override
        public BigDecimal desconto(ContextoCupom contexto) {
            return contexto.frete();
        }
    },

    LEVE3PAGUE2 {
        @Override
        public BigDecimal desconto(ContextoCupom contexto) {
            BigDecimal total = contexto.itens().stream()
                    .map(item -> item.precoUnitario().multiply(BigDecimal.valueOf(item.quantidade() / 3)))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            return Dinheiro.centavos(total);
        }
    };

    public abstract BigDecimal desconto(ContextoCupom contexto);

    /** Se o cupom existe mas o pedido não cumpre a condição. */
    public boolean aplicavel(ContextoCupom contexto) {
        return true;
    }
}
