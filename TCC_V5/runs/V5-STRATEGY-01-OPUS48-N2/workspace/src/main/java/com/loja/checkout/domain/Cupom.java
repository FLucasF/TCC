package com.loja.checkout.domain;

import com.loja.checkout.dinheiro.Dinheiro;
import com.loja.checkout.web.ItemPedido;

import java.math.BigDecimal;

/**
 * Cupons de promoção. A condição para valer e o cálculo do desconto são o que
 * muda de um para outro, então cada cupom guarda os seus. Promoção nova é uma
 * constante nova aqui.
 */
public enum Cupom {

    BEMVINDO10 {
        @Override
        public BigDecimal desconto(CupomContexto ctx) {
            return Dinheiro.arredondar(new BigDecimal("0.10").multiply(ctx.subtotalProdutos()));
        }
    },

    MENOS50 {
        @Override
        public boolean aplicavel(CupomContexto ctx) {
            return ctx.subtotalProdutos().compareTo(new BigDecimal("300")) >= 0;
        }

        @Override
        public BigDecimal desconto(CupomContexto ctx) {
            return Dinheiro.arredondar(new BigDecimal("50"));
        }
    },

    FRETEGRATIS {
        @Override
        public BigDecimal desconto(CupomContexto ctx) {
            return Dinheiro.arredondar(ctx.frete());
        }
    },

    LEVE3PAGUE2 {
        @Override
        public BigDecimal desconto(CupomContexto ctx) {
            BigDecimal gratis = BigDecimal.ZERO;
            for (ItemPedido item : ctx.itens()) {
                int unidadesGratis = item.quantidade() / 3;
                gratis = gratis.add(item.precoUnitario().multiply(BigDecimal.valueOf(unidadesGratis)));
            }
            return Dinheiro.arredondar(gratis);
        }
    };

    /** Se o pedido cumpre a condição do cupom. */
    public boolean aplicavel(CupomContexto ctx) {
        return true;
    }

    /** Valor do desconto do cupom, já em centavos. */
    public abstract BigDecimal desconto(CupomContexto ctx);
}
