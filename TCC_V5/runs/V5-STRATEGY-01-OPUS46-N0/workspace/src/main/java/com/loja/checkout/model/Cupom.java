package com.loja.checkout.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import com.loja.checkout.dto.ItemRequest;

public enum Cupom {

    BEMVINDO10 {
        @Override
        public BigDecimal calcularDesconto(BigDecimal subtotal, BigDecimal frete, List<ItemRequest> itens) {
            return subtotal.multiply(new BigDecimal("0.10")).setScale(2, RoundingMode.HALF_EVEN);
        }

        @Override
        public boolean aplicavel(BigDecimal subtotal, List<ItemRequest> itens) {
            return true;
        }
    },

    MENOS50 {
        @Override
        public BigDecimal calcularDesconto(BigDecimal subtotal, BigDecimal frete, List<ItemRequest> itens) {
            return new BigDecimal("50.00");
        }

        @Override
        public boolean aplicavel(BigDecimal subtotal, List<ItemRequest> itens) {
            return subtotal.compareTo(new BigDecimal("300.00")) >= 0;
        }
    },

    FRETEGRATIS {
        @Override
        public BigDecimal calcularDesconto(BigDecimal subtotal, BigDecimal frete, List<ItemRequest> itens) {
            return frete;
        }

        @Override
        public boolean aplicavel(BigDecimal subtotal, List<ItemRequest> itens) {
            return true;
        }
    },

    LEVE3PAGUE2 {
        @Override
        public BigDecimal calcularDesconto(BigDecimal subtotal, BigDecimal frete, List<ItemRequest> itens) {
            BigDecimal desconto = BigDecimal.ZERO;
            for (ItemRequest item : itens) {
                int gratis = item.quantidade() / 3;
                if (gratis > 0) {
                    desconto = desconto.add(item.precoUnitario().multiply(new BigDecimal(gratis)));
                }
            }
            return desconto.setScale(2, RoundingMode.HALF_EVEN);
        }

        @Override
        public boolean aplicavel(BigDecimal subtotal, List<ItemRequest> itens) {
            return true;
        }
    };

    public abstract BigDecimal calcularDesconto(BigDecimal subtotal, BigDecimal frete, List<ItemRequest> itens);

    public abstract boolean aplicavel(BigDecimal subtotal, List<ItemRequest> itens);
}
