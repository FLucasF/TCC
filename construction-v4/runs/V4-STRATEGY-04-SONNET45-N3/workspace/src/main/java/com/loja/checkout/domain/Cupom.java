package com.loja.checkout.domain;

import com.loja.checkout.dto.ItemPedido;
import com.loja.checkout.exception.CheckoutException;
import java.math.BigDecimal;
import java.util.List;

public enum Cupom {
    BEMVINDO10 {
        @Override
        public void validarAplicabilidade(BigDecimal subtotalProdutos, List<ItemPedido> itens, BigDecimal frete) {
        }

        @Override
        public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, List<ItemPedido> itens, BigDecimal frete) {
            return subtotalProdutos.multiply(new BigDecimal("0.10"))
                .setScale(2, java.math.RoundingMode.HALF_EVEN);
        }
    },
    MENOS50 {
        @Override
        public void validarAplicabilidade(BigDecimal subtotalProdutos, List<ItemPedido> itens, BigDecimal frete) {
            if (subtotalProdutos.compareTo(new BigDecimal("300.00")) < 0) {
                throw new CheckoutException("CUPOM_NAO_APLICAVEL");
            }
        }

        @Override
        public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, List<ItemPedido> itens, BigDecimal frete) {
            return new BigDecimal("50.00").setScale(2, java.math.RoundingMode.HALF_EVEN);
        }
    },
    FRETEGRATIS {
        @Override
        public void validarAplicabilidade(BigDecimal subtotalProdutos, List<ItemPedido> itens, BigDecimal frete) {
        }

        @Override
        public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, List<ItemPedido> itens, BigDecimal frete) {
            return frete;
        }
    },
    LEVE3PAGUE2 {
        @Override
        public void validarAplicabilidade(BigDecimal subtotalProdutos, List<ItemPedido> itens, BigDecimal frete) {
        }

        @Override
        public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, List<ItemPedido> itens, BigDecimal frete) {
            BigDecimal desconto = BigDecimal.ZERO;
            for (ItemPedido item : itens) {
                int unidadesGratis = item.quantidade() / 3;
                BigDecimal descontoItem = item.precoUnitario()
                    .multiply(BigDecimal.valueOf(unidadesGratis))
                    .setScale(2, java.math.RoundingMode.HALF_EVEN);
                desconto = desconto.add(descontoItem);
            }
            return desconto.setScale(2, java.math.RoundingMode.HALF_EVEN);
        }
    };

    public abstract void validarAplicabilidade(BigDecimal subtotalProdutos, List<ItemPedido> itens, BigDecimal frete);
    public abstract BigDecimal calcularDesconto(BigDecimal subtotalProdutos, List<ItemPedido> itens, BigDecimal frete);

    public static Cupom fromString(String valor) {
        if (valor == null || valor.isEmpty()) {
            return null;
        }
        try {
            return Cupom.valueOf(valor);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException("CUPOM_INVALIDO");
        }
    }
}
