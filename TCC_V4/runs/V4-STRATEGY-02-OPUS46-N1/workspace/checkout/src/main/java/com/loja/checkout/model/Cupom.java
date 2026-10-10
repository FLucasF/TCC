package com.loja.checkout.model;

import com.loja.checkout.dto.ItemRequest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public enum Cupom {

    BEMVINDO10 {
        @Override
        public boolean isAplicavel(BigDecimal subtotal, List<ItemRequest> itens) {
            return true;
        }

        @Override
        public BigDecimal calcularDesconto(BigDecimal subtotal, List<ItemRequest> itens, BigDecimal frete) {
            return subtotal.multiply(new BigDecimal("0.10")).setScale(2, RoundingMode.HALF_EVEN);
        }
    },
    MENOS50 {
        @Override
        public boolean isAplicavel(BigDecimal subtotal, List<ItemRequest> itens) {
            return subtotal.compareTo(new BigDecimal("300.00")) >= 0;
        }

        @Override
        public BigDecimal calcularDesconto(BigDecimal subtotal, List<ItemRequest> itens, BigDecimal frete) {
            return new BigDecimal("50.00");
        }
    },
    FRETEGRATIS {
        @Override
        public boolean isAplicavel(BigDecimal subtotal, List<ItemRequest> itens) {
            return true;
        }

        @Override
        public BigDecimal calcularDesconto(BigDecimal subtotal, List<ItemRequest> itens, BigDecimal frete) {
            return frete;
        }
    },
    LEVE3PAGUE2 {
        @Override
        public boolean isAplicavel(BigDecimal subtotal, List<ItemRequest> itens) {
            return true;
        }

        @Override
        public BigDecimal calcularDesconto(BigDecimal subtotal, List<ItemRequest> itens, BigDecimal frete) {
            BigDecimal desconto = BigDecimal.ZERO;
            for (ItemRequest item : itens) {
                int gratis = item.quantidade() / 3;
                desconto = desconto.add(item.precoUnitario().multiply(BigDecimal.valueOf(gratis)));
            }
            return desconto.setScale(2, RoundingMode.HALF_EVEN);
        }
    };

    public abstract boolean isAplicavel(BigDecimal subtotal, List<ItemRequest> itens);

    public abstract BigDecimal calcularDesconto(BigDecimal subtotal, List<ItemRequest> itens, BigDecimal frete);
}
