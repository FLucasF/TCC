package com.loja.checkout.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Cupom de desconto. Cada promoção tem sua condição e sua conta de desconto, e
 * mora no seu próprio membro; promoção nova = novo membro. O desconto incide
 * sobre os produtos, exceto FRETEGRATIS, que iguala o valor do frete — por isso
 * o contexto carrega subtotal, frete e os itens.
 */
public enum Cupom {

    BEMVINDO10 {
        @Override
        public BigDecimal desconto(Contexto ctx) {
            return Dinheiro.centavos(ctx.subtotalProdutos().multiply(new BigDecimal("0.10")));
        }
    },
    MENOS50 {
        @Override
        public boolean aplicavel(Contexto ctx) {
            return ctx.subtotalProdutos().compareTo(new BigDecimal("300")) >= 0;
        }

        @Override
        public BigDecimal desconto(Contexto ctx) {
            return Dinheiro.centavos(new BigDecimal("50"));
        }
    },
    FRETEGRATIS {
        @Override
        public BigDecimal desconto(Contexto ctx) {
            return ctx.frete();
        }
    },
    LEVE3PAGUE2 {
        @Override
        public BigDecimal desconto(Contexto ctx) {
            BigDecimal gratis = BigDecimal.ZERO;
            for (ItemPedido item : ctx.itens()) {
                int unidadesGratis = item.quantidade() / 3;
                gratis = gratis.add(item.precoUnitario().multiply(BigDecimal.valueOf(unidadesGratis)));
            }
            return Dinheiro.centavos(gratis);
        }
    };

    public boolean aplicavel(Contexto ctx) {
        return true;
    }

    public abstract BigDecimal desconto(Contexto ctx);

    public static Optional<Cupom> resolver(String codigo) {
        return Catalogo.achar(Cupom.class, codigo);
    }

    /** Dados que algum cupom precisa para calcular condição e desconto. */
    public record Contexto(BigDecimal subtotalProdutos, BigDecimal frete, List<ItemPedido> itens) {
    }
}
