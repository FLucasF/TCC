package com.loja.checkout;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public enum Cupom {

    BEMVINDO10 {
        @Override
        public BigDecimal calcularDesconto(List<ItemCarrinho> itens, BigDecimal subtotal, BigDecimal frete) {
            return subtotal.multiply(new BigDecimal("0.10")).setScale(2, RoundingMode.HALF_EVEN);
        }

        @Override
        public boolean isAplicavel(List<ItemCarrinho> itens, BigDecimal subtotal) {
            return true;
        }
    },

    MENOS50 {
        @Override
        public BigDecimal calcularDesconto(List<ItemCarrinho> itens, BigDecimal subtotal, BigDecimal frete) {
            return new BigDecimal("50.00");
        }

        @Override
        public boolean isAplicavel(List<ItemCarrinho> itens, BigDecimal subtotal) {
            return subtotal.compareTo(new BigDecimal("300.00")) >= 0;
        }
    },

    FRETEGRATIS {
        @Override
        public BigDecimal calcularDesconto(List<ItemCarrinho> itens, BigDecimal subtotal, BigDecimal frete) {
            return frete;
        }

        @Override
        public boolean isAplicavel(List<ItemCarrinho> itens, BigDecimal subtotal) {
            return true;
        }
    },

    LEVE3PAGUE2 {
        @Override
        public BigDecimal calcularDesconto(List<ItemCarrinho> itens, BigDecimal subtotal, BigDecimal frete) {
            BigDecimal desconto = BigDecimal.ZERO;
            for (ItemCarrinho item : itens) {
                int gratuitos = item.quantidade() / 3;
                desconto = desconto.add(item.precoUnitario().multiply(BigDecimal.valueOf(gratuitos)));
            }
            return desconto.setScale(2, RoundingMode.HALF_EVEN);
        }

        @Override
        public boolean isAplicavel(List<ItemCarrinho> itens, BigDecimal subtotal) {
            return true;
        }
    };

    public abstract BigDecimal calcularDesconto(List<ItemCarrinho> itens, BigDecimal subtotal, BigDecimal frete);

    public abstract boolean isAplicavel(List<ItemCarrinho> itens, BigDecimal subtotal);
}
