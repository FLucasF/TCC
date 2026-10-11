package com.loja.checkout.domain;

import com.loja.checkout.dto.ItemRequest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public enum Cupom {

    BEMVINDO10 {
        @Override
        public boolean aplicavel(List<ItemRequest> itens, BigDecimal subtotal) {
            return true;
        }

        @Override
        public BigDecimal calcularDesconto(List<ItemRequest> itens, BigDecimal subtotal, BigDecimal frete) {
            return round(subtotal.multiply(new BigDecimal("0.10")));
        }
    },

    MENOS50 {
        @Override
        public boolean aplicavel(List<ItemRequest> itens, BigDecimal subtotal) {
            return subtotal.compareTo(new BigDecimal("300.00")) >= 0;
        }

        @Override
        public BigDecimal calcularDesconto(List<ItemRequest> itens, BigDecimal subtotal, BigDecimal frete) {
            return new BigDecimal("50.00");
        }
    },

    FRETEGRATIS {
        @Override
        public boolean aplicavel(List<ItemRequest> itens, BigDecimal subtotal) {
            return true;
        }

        @Override
        public BigDecimal calcularDesconto(List<ItemRequest> itens, BigDecimal subtotal, BigDecimal frete) {
            return frete;
        }
    },

    LEVE3PAGUE2 {
        @Override
        public boolean aplicavel(List<ItemRequest> itens, BigDecimal subtotal) {
            return true;
        }

        @Override
        public BigDecimal calcularDesconto(List<ItemRequest> itens, BigDecimal subtotal, BigDecimal frete) {
            BigDecimal desconto = BigDecimal.ZERO;
            for (ItemRequest item : itens) {
                int gratuitos = item.quantidade() / 3;
                if (gratuitos > 0) {
                    desconto = desconto.add(item.precoUnitario().multiply(new BigDecimal(gratuitos)));
                }
            }
            return round(desconto);
        }
    };

    public abstract boolean aplicavel(List<ItemRequest> itens, BigDecimal subtotal);

    public abstract BigDecimal calcularDesconto(List<ItemRequest> itens, BigDecimal subtotal, BigDecimal frete);

    private static BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }
}
