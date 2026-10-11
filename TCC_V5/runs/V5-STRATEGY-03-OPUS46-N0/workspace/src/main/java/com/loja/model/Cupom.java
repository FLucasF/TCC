package com.loja.model;

import com.loja.dto.ItemRequest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public enum Cupom {

    BEMVINDO10 {
        @Override
        public BigDecimal calcularDesconto(BigDecimal subtotal, List<ItemRequest> itens, BigDecimal frete) {
            return subtotal.multiply(new BigDecimal("0.10")).setScale(2, RoundingMode.HALF_EVEN);
        }
    },

    MENOS50 {
        @Override
        public boolean isAplicavel(BigDecimal subtotal) {
            return subtotal.compareTo(new BigDecimal("300")) >= 0;
        }

        @Override
        public BigDecimal calcularDesconto(BigDecimal subtotal, List<ItemRequest> itens, BigDecimal frete) {
            return new BigDecimal("50.00");
        }
    },

    FRETEGRATIS {
        @Override
        public BigDecimal calcularDesconto(BigDecimal subtotal, List<ItemRequest> itens, BigDecimal frete) {
            return frete;
        }
    },

    LEVE3PAGUE2 {
        @Override
        public BigDecimal calcularDesconto(BigDecimal subtotal, List<ItemRequest> itens, BigDecimal frete) {
            BigDecimal desconto = BigDecimal.ZERO;
            for (ItemRequest item : itens) {
                int gratis = item.quantidade() / 3;
                desconto = desconto.add(item.precoUnitario().multiply(new BigDecimal(gratis)));
            }
            return desconto.setScale(2, RoundingMode.HALF_EVEN);
        }
    };

    public boolean isAplicavel(BigDecimal subtotal) {
        return true;
    }

    public abstract BigDecimal calcularDesconto(BigDecimal subtotal, List<ItemRequest> itens, BigDecimal frete);
}
