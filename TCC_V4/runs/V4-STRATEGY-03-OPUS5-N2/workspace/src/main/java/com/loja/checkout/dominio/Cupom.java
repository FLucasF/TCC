package com.loja.checkout.dominio;

import java.math.BigDecimal;

/**
 * Cupons de promocao. Cada promocao decide sozinha se vale para o pedido e
 * quanto desconta, porque cada uma desconta de um jeito diferente: criar uma
 * promocao nova e acrescentar uma constante aqui.
 */
public enum Cupom {

    BEMVINDO10 {
        @Override
        public BigDecimal desconto(Carrinho carrinho, BigDecimal frete) {
            return Dinheiro.percentual(carrinho.subtotalProdutos(), new BigDecimal("0.10"));
        }
    },

    MENOS50 {
        private static final BigDecimal PRODUTOS_MINIMOS = new BigDecimal("300.00");

        @Override
        public boolean aplicavel(Carrinho carrinho, BigDecimal frete) {
            return carrinho.subtotalProdutos().compareTo(PRODUTOS_MINIMOS) >= 0;
        }

        @Override
        public BigDecimal desconto(Carrinho carrinho, BigDecimal frete) {
            return Dinheiro.arredondar(new BigDecimal("50.00"));
        }
    },

    FRETEGRATIS {
        @Override
        public BigDecimal desconto(Carrinho carrinho, BigDecimal frete) {
            return Dinheiro.arredondar(frete);
        }
    },

    LEVE3PAGUE2 {
        private static final int UNIDADES_PARA_GANHAR_UMA = 3;

        @Override
        public BigDecimal desconto(Carrinho carrinho, BigDecimal frete) {
            BigDecimal gratis = carrinho.itens().stream()
                    .map(item -> item.precoUnitario()
                            .multiply(BigDecimal.valueOf(item.quantidade() / UNIDADES_PARA_GANHAR_UMA)))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            return Dinheiro.arredondar(gratis);
        }
    };

    /** Por padrao a promocao vale para qualquer pedido. */
    public boolean aplicavel(Carrinho carrinho, BigDecimal frete) {
        return true;
    }

    public abstract BigDecimal desconto(Carrinho carrinho, BigDecimal frete);
}
