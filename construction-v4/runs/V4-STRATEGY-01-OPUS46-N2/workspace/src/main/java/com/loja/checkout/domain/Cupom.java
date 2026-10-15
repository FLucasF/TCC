package com.loja.checkout.domain;

import com.loja.checkout.dto.ItemRequest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public enum Cupom {

    BEMVINDO10 {
        @Override
        public boolean aplicavel(BigDecimal subtotal, List<ItemRequest> itens) {
            return true;
        }

        @Override
        public BigDecimal calcularDesconto(BigDecimal subtotal, BigDecimal frete, List<ItemRequest> itens) {
            return arredondar(subtotal.multiply(new BigDecimal("0.10")));
        }
    },

    MENOS50 {
        @Override
        public boolean aplicavel(BigDecimal subtotal, List<ItemRequest> itens) {
            return subtotal.compareTo(new BigDecimal("300")) >= 0;
        }

        @Override
        public BigDecimal calcularDesconto(BigDecimal subtotal, BigDecimal frete, List<ItemRequest> itens) {
            return new BigDecimal("50.00");
        }
    },

    FRETEGRATIS {
        @Override
        public boolean aplicavel(BigDecimal subtotal, List<ItemRequest> itens) {
            return true;
        }

        @Override
        public BigDecimal calcularDesconto(BigDecimal subtotal, BigDecimal frete, List<ItemRequest> itens) {
            return frete;
        }
    },

    LEVE3PAGUE2 {
        @Override
        public boolean aplicavel(BigDecimal subtotal, List<ItemRequest> itens) {
            return true;
        }

        @Override
        public BigDecimal calcularDesconto(BigDecimal subtotal, BigDecimal frete, List<ItemRequest> itens) {
            BigDecimal desconto = BigDecimal.ZERO;
            for (ItemRequest item : itens) {
                int gratis = item.quantidade() / 3;
                desconto = desconto.add(item.precoUnitario().multiply(new BigDecimal(gratis)));
            }
            return arredondar(desconto);
        }
    };

    public abstract boolean aplicavel(BigDecimal subtotal, List<ItemRequest> itens);

    public abstract BigDecimal calcularDesconto(BigDecimal subtotal, BigDecimal frete, List<ItemRequest> itens);

    private static BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }
}
