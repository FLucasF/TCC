package com.loja.resumo.calculo;

import java.math.BigDecimal;

public enum Cupom {

    BEMVINDO10 {
        @Override
        boolean aplicavel(Carrinho carrinho) {
            return true;
        }

        @Override
        BigDecimal desconto(Carrinho carrinho, BigDecimal frete) {
            return Dinheiro.sobre(carrinho.subtotal(), new BigDecimal("0.10"));
        }
    },
    MENOS50 {
        @Override
        boolean aplicavel(Carrinho carrinho) {
            return carrinho.subtotal().compareTo(new BigDecimal("300")) >= 0;
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
            return carrinho.itens().stream()
                    .map(item -> Dinheiro.centavos(item.precoUnitario().multiply(BigDecimal.valueOf(item.quantidade() / 3))))
                    .reduce(Dinheiro.ZERO, BigDecimal::add);
        }
    };

    abstract boolean aplicavel(Carrinho carrinho);

    abstract BigDecimal desconto(Carrinho carrinho, BigDecimal frete);
}
