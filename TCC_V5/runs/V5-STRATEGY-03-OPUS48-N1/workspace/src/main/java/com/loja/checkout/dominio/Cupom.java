package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.List;

/**
 * Cupons de desconto. Cada promocao tem a propria conta de desconto e a
 * propria condicao de uso, entao cada caso mora no proprio corpo do enum.
 * Promocao nova entra como uma nova constante.
 */
public enum Cupom {

    /** 10% sobre o valor dos produtos. */
    BEMVINDO10 {
        @Override
        public BigDecimal desconto(Contexto contexto) {
            return contexto.subtotalProdutos().multiply(new BigDecimal("0.10"));
        }
    },

    /** R$ 50,00 fixos, so a partir de R$ 300,00 em produtos. */
    MENOS50 {
        @Override
        public boolean aplicavel(BigDecimal subtotalProdutos) {
            return subtotalProdutos.compareTo(new BigDecimal("300.00")) >= 0;
        }

        @Override
        public BigDecimal desconto(Contexto contexto) {
            return new BigDecimal("50.00");
        }
    },

    /** O cliente nao paga frete: o desconto fica igual ao frete do resumo. */
    FRETEGRATIS {
        @Override
        public BigDecimal desconto(Contexto contexto) {
            return contexto.frete();
        }
    },

    /** A cada 3 unidades de um mesmo item, uma sai de graca. */
    LEVE3PAGUE2 {
        @Override
        public BigDecimal desconto(Contexto contexto) {
            BigDecimal total = BigDecimal.ZERO;
            for (ItemPedido item : contexto.itens()) {
                int gratis = item.quantidade() / 3;
                total = total.add(item.precoUnitario().multiply(BigDecimal.valueOf(gratis)));
            }
            return total;
        }
    };

    /** Se o pedido cumpre a condicao do cupom. Por padrao, cumpre. */
    public boolean aplicavel(BigDecimal subtotalProdutos) {
        return true;
    }

    /** Desconto bruto do cupom; quem chama arredonda para centavos. */
    public abstract BigDecimal desconto(Contexto contexto);

    /**
     * O que um cupom pode precisar para calcular o desconto: o valor dos
     * produtos, o frete do resumo e os itens do carrinho.
     */
    public record Contexto(BigDecimal subtotalProdutos, BigDecimal frete, List<ItemPedido> itens) {
    }
}
