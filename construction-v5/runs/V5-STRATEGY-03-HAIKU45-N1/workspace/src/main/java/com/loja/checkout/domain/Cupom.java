package com.loja.checkout.domain;

import com.loja.checkout.dto.Item;
import java.math.BigDecimal;
import java.util.List;

public enum Cupom {
    BEMVINDO10 {
        @Override
        public boolean validar(BigDecimal subtotal, List<Item> itens) {
            return true;
        }

        @Override
        public BigDecimal calcularDesconto(BigDecimal subtotal, List<Item> itens, BigDecimal frete) {
            return subtotal.multiply(BigDecimal.valueOf(0.1));
        }
    },
    MENOS50 {
        @Override
        public boolean validar(BigDecimal subtotal, List<Item> itens) {
            return subtotal.compareTo(BigDecimal.valueOf(300)) >= 0;
        }

        @Override
        public BigDecimal calcularDesconto(BigDecimal subtotal, List<Item> itens, BigDecimal frete) {
            return BigDecimal.valueOf(50);
        }
    },
    FRETEGRATIS {
        @Override
        public boolean validar(BigDecimal subtotal, List<Item> itens) {
            return true;
        }

        @Override
        public BigDecimal calcularDesconto(BigDecimal subtotal, List<Item> itens, BigDecimal frete) {
            return frete;
        }
    },
    LEVE3PAGUE2 {
        @Override
        public boolean validar(BigDecimal subtotal, List<Item> itens) {
            return true;
        }

        @Override
        public BigDecimal calcularDesconto(BigDecimal subtotal, List<Item> itens, BigDecimal frete) {
            BigDecimal desconto = BigDecimal.ZERO;
            for (Item item : itens) {
                int gratuitas = item.getQuantidade() / 3;
                if (gratuitas > 0) {
                    desconto = desconto.add(item.getPrecoUnitario().multiply(BigDecimal.valueOf(gratuitas)));
                }
            }
            return desconto;
        }
    };

    public abstract boolean validar(BigDecimal subtotal, List<Item> itens);
    public abstract BigDecimal calcularDesconto(BigDecimal subtotal, List<Item> itens, BigDecimal frete);

    public static Cupom fromString(String value) {
        if (value == null) {
            return null;
        }
        try {
            return Cupom.valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
