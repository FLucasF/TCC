package com.loja.checkout.dominio;

import java.math.BigDecimal;

/**
 * Cupons da loja. Cada promoção guarda a sua própria condição e a sua própria
 * conta de desconto, então uma promoção nova é só uma constante nova.
 *
 * <p>Só o MENOS50 tem condição: é a única promoção que o enunciado descreve com
 * exigência. As outras valem sempre, mesmo quando o desconto sai zerado — é o
 * caso do FRETEGRATIS numa retirada na loja.
 */
public enum Cupom {

    BEMVINDO10 {
        private static final BigDecimal PERCENTUAL = new BigDecimal("0.10");

        @Override
        public BigDecimal desconto(ContextoCupom contexto) {
            return Dinheiro.percentual(contexto.subtotalProdutos(), PERCENTUAL);
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
        private static final int UNIDADES_PARA_UMA_GRATIS = 3;

        @Override
        public BigDecimal desconto(ContextoCupom contexto) {
            return contexto.itens().stream()
                    .map(item -> item.valorDe(item.quantidade() / UNIDADES_PARA_UMA_GRATIS))
                    .reduce(Dinheiro.ZERO, BigDecimal::add);
        }
    };

    /** Se o pedido cumpre a condição da promoção. */
    public boolean aplicavel(ContextoCupom contexto) {
        return true;
    }

    /** Desconto da promoção, em centavos. */
    public abstract BigDecimal desconto(ContextoCupom contexto);
}
