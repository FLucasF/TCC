package com.loja.checkout.dominio;

import java.math.BigDecimal;

public enum Cupom {

    BEMVINDO10 {
        public BigDecimal desconto(Carrinho carrinho, BigDecimal frete) {
            return Dinheiro.porcentagem(carrinho.subtotal(), "0.10");
        }
    },
    MENOS50 {
        public void verificarAplicavel(Carrinho carrinho) {
            if (carrinho.subtotal().compareTo(new BigDecimal("300.00")) < 0) {
                throw new ErroPedido(CodigoErro.CUPOM_NAO_APLICAVEL);
            }
        }

        public BigDecimal desconto(Carrinho carrinho, BigDecimal frete) {
            return new BigDecimal("50.00");
        }
    },
    FRETEGRATIS {
        public BigDecimal desconto(Carrinho carrinho, BigDecimal frete) {
            return frete;
        }
    },
    LEVE3PAGUE2 {
        public BigDecimal desconto(Carrinho carrinho, BigDecimal frete) {
            return carrinho.itens().stream()
                    .map(item -> item.precoUnitario()
                            .multiply(BigDecimal.valueOf(item.quantidade() / 3)))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
    };

    public void verificarAplicavel(Carrinho carrinho) {
    }

    public abstract BigDecimal desconto(Carrinho carrinho, BigDecimal frete);
}
