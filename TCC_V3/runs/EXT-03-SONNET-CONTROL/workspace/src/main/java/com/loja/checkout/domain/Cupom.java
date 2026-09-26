package com.loja.checkout.domain;

import com.loja.checkout.dto.ItemRequest;

import java.math.BigDecimal;
import java.util.List;

public enum Cupom {

    BEMVINDO10 {
        @Override
        public boolean aplicavel(List<ItemRequest> itens, BigDecimal subtotal, BigDecimal frete) {
            return true;
        }

        @Override
        public BigDecimal calcularDesconto(List<ItemRequest> itens, BigDecimal subtotal, BigDecimal frete) {
            return subtotal.multiply(new BigDecimal("0.10"));
        }
    },
    MENOS50 {
        @Override
        public boolean aplicavel(List<ItemRequest> itens, BigDecimal subtotal, BigDecimal frete) {
            return subtotal.compareTo(new BigDecimal("300.00")) >= 0;
        }

        @Override
        public BigDecimal calcularDesconto(List<ItemRequest> itens, BigDecimal subtotal, BigDecimal frete) {
            return new BigDecimal("50.00");
        }
    },
    FRETEGRATIS {
        @Override
        public boolean aplicavel(List<ItemRequest> itens, BigDecimal subtotal, BigDecimal frete) {
            return true;
        }

        @Override
        public BigDecimal calcularDesconto(List<ItemRequest> itens, BigDecimal subtotal, BigDecimal frete) {
            return frete;
        }
    },
    LEVE3PAGUE2 {
        @Override
        public boolean aplicavel(List<ItemRequest> itens, BigDecimal subtotal, BigDecimal frete) {
            return true;
        }

        @Override
        public BigDecimal calcularDesconto(List<ItemRequest> itens, BigDecimal subtotal, BigDecimal frete) {
            BigDecimal desconto = BigDecimal.ZERO;
            for (ItemRequest item : itens) {
                int unidadesGratis = item.getQuantidade() / 3;
                desconto = desconto.add(item.getPrecoUnitario().multiply(BigDecimal.valueOf(unidadesGratis)));
            }
            return desconto;
        }
    };

    public abstract boolean aplicavel(List<ItemRequest> itens, BigDecimal subtotal, BigDecimal frete);

    public abstract BigDecimal calcularDesconto(List<ItemRequest> itens, BigDecimal subtotal, BigDecimal frete);
}
