package com.loja.checkout;

import java.math.BigDecimal;

public enum Cupom {

    BEMVINDO10 {
        @Override
        BigDecimal desconto(Carrinho carrinho, BigDecimal frete) {
            return carrinho.subtotal().multiply(new BigDecimal("0.10"));
        }
    },
    MENOS50 {
        @Override
        boolean aplicaA(Carrinho carrinho) {
            return carrinho.subtotal().compareTo(new BigDecimal("300")) >= 0;
        }

        @Override
        BigDecimal desconto(Carrinho carrinho, BigDecimal frete) {
            return new BigDecimal("50.00");
        }
    },
    FRETEGRATIS {
        @Override
        BigDecimal desconto(Carrinho carrinho, BigDecimal frete) {
            return frete;
        }
    },
    LEVE3PAGUE2 {
        @Override
        boolean aplicaA(Carrinho carrinho) {
            return carrinho.itens().stream().anyMatch(item -> item.quantidade() >= 3);
        }

        @Override
        BigDecimal desconto(Carrinho carrinho, BigDecimal frete) {
            return carrinho.itens().stream()
                    .map(item -> item.precoUnitario().multiply(BigDecimal.valueOf(item.quantidade() / 3)))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
    };

    boolean aplicaA(Carrinho carrinho) {
        return true;
    }

    abstract BigDecimal desconto(Carrinho carrinho, BigDecimal frete);

    static Cupom de(String codigo) {
        return ResumoException.paraEnum(values(), codigo, "CUPOM_INVALIDO");
    }
}
