package com.loja.checkout.domain;

import java.math.BigDecimal;
import java.util.List;

import static com.loja.checkout.domain.Arredondamento.r;

public enum Cupom {

    BEMVINDO10 {
        @Override public void validarAplicabilidade(BigDecimal subtotal) {}

        @Override public BigDecimal calcularDesconto(BigDecimal subtotal, BigDecimal frete, List<ItemValor> itens) {
            return r(subtotal.multiply(new BigDecimal("0.10")));
        }
    },

    MENOS50 {
        @Override public void validarAplicabilidade(BigDecimal subtotal) {
            if (subtotal.compareTo(new BigDecimal("300")) < 0)
                throw new CheckoutException("CUPOM_NAO_APLICAVEL");
        }

        @Override public BigDecimal calcularDesconto(BigDecimal subtotal, BigDecimal frete, List<ItemValor> itens) {
            return new BigDecimal("50.00");
        }
    },

    FRETEGRATIS {
        @Override public void validarAplicabilidade(BigDecimal subtotal) {}

        @Override public BigDecimal calcularDesconto(BigDecimal subtotal, BigDecimal frete, List<ItemValor> itens) {
            return frete;
        }
    },

    LEVE3PAGUE2 {
        @Override public void validarAplicabilidade(BigDecimal subtotal) {}

        @Override public BigDecimal calcularDesconto(BigDecimal subtotal, BigDecimal frete, List<ItemValor> itens) {
            BigDecimal desconto = itens.stream()
                    .map(item -> item.precoUnitario().multiply(BigDecimal.valueOf(item.quantidade() / 3)))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            return r(desconto);
        }
    };

    public abstract void validarAplicabilidade(BigDecimal subtotal);

    public abstract BigDecimal calcularDesconto(BigDecimal subtotal, BigDecimal frete, List<ItemValor> itens);
}
