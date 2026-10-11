package br.com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.List;

public enum Cupom {

    BEMVINDO10 {
        @Override
        public BigDecimal desconto(List<Item> itens, BigDecimal subtotal, BigDecimal frete) {
            return subtotal.multiply(new BigDecimal("0.10"));
        }
    },

    MENOS50 {
        @Override
        public boolean aplicavel(BigDecimal subtotal) {
            return subtotal.compareTo(new BigDecimal("300.00")) >= 0;
        }

        @Override
        public BigDecimal desconto(List<Item> itens, BigDecimal subtotal, BigDecimal frete) {
            return new BigDecimal("50.00");
        }
    },

    FRETEGRATIS {
        @Override
        public BigDecimal desconto(List<Item> itens, BigDecimal subtotal, BigDecimal frete) {
            return frete;
        }
    },

    LEVE3PAGUE2 {
        @Override
        public BigDecimal desconto(List<Item> itens, BigDecimal subtotal, BigDecimal frete) {
            return itens.stream()
                    .map(item -> item.precoUnitario().multiply(BigDecimal.valueOf(item.quantidade() / 3)))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
    };

    public boolean aplicavel(BigDecimal subtotal) {
        return true;
    }

    public abstract BigDecimal desconto(List<Item> itens, BigDecimal subtotal, BigDecimal frete);
}
