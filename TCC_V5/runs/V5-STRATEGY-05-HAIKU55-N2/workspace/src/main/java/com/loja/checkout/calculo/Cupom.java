package com.loja.checkout.calculo;

import java.math.BigDecimal;

import static com.loja.checkout.calculo.Dinheiro.centavos;

enum Cupom {

    BEMVINDO10 {
        @Override
        boolean aplicavel(Carrinho carrinho) {
            return true;
        }

        @Override
        BigDecimal desconto(Carrinho carrinho, BigDecimal frete) {
            return centavos(carrinho.subtotal().multiply(new BigDecimal("0.10")));
        }
    },
    MENOS50 {
        @Override
        boolean aplicavel(Carrinho carrinho) {
            return carrinho.subtotal().compareTo(new BigDecimal("300.00")) >= 0;
        }

        @Override
        BigDecimal desconto(Carrinho carrinho, BigDecimal frete) {
            return new BigDecimal("50.00");
        }
    },
    FRETEGRATIS {
        @Override
        boolean aplicavel(Carrinho carrinho) {
            return true;
        }

        @Override
        BigDecimal desconto(Carrinho carrinho, BigDecimal frete) {
            return frete;
        }
    },
    LEVE3PAGUE2 {
        @Override
        boolean aplicavel(Carrinho carrinho) {
            return carrinho.itens().stream().anyMatch(item -> item.quantidade() >= 3);
        }

        @Override
        BigDecimal desconto(Carrinho carrinho, BigDecimal frete) {
            BigDecimal soma = carrinho.itens().stream()
                    .map(item -> item.precoUnitario().multiply(BigDecimal.valueOf(item.quantidade() / 3)))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            return centavos(soma);
        }
    };

    abstract boolean aplicavel(Carrinho carrinho);

    abstract BigDecimal desconto(Carrinho carrinho, BigDecimal frete);
}
