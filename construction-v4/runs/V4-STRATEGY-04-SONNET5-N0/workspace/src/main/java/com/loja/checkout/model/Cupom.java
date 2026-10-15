package com.loja.checkout.model;

import com.loja.checkout.dto.ItemDto;
import com.loja.checkout.util.Dinheiro;

import java.math.BigDecimal;
import java.util.List;

public enum Cupom {

    BEMVINDO10 {
        @Override
        public boolean aplicavel(List<ItemDto> itens, BigDecimal subtotalProdutos) {
            return true;
        }

        @Override
        public BigDecimal desconto(List<ItemDto> itens, BigDecimal subtotalProdutos, BigDecimal frete) {
            return Dinheiro.round(subtotalProdutos.multiply(new BigDecimal("0.10")));
        }
    },
    MENOS50 {
        @Override
        public boolean aplicavel(List<ItemDto> itens, BigDecimal subtotalProdutos) {
            return subtotalProdutos.compareTo(new BigDecimal("300")) >= 0;
        }

        @Override
        public BigDecimal desconto(List<ItemDto> itens, BigDecimal subtotalProdutos, BigDecimal frete) {
            return new BigDecimal("50.00");
        }
    },
    FRETEGRATIS {
        @Override
        public boolean aplicavel(List<ItemDto> itens, BigDecimal subtotalProdutos) {
            return true;
        }

        @Override
        public BigDecimal desconto(List<ItemDto> itens, BigDecimal subtotalProdutos, BigDecimal frete) {
            return frete;
        }
    },
    LEVE3PAGUE2 {
        @Override
        public boolean aplicavel(List<ItemDto> itens, BigDecimal subtotalProdutos) {
            return true;
        }

        @Override
        public BigDecimal desconto(List<ItemDto> itens, BigDecimal subtotalProdutos, BigDecimal frete) {
            BigDecimal total = BigDecimal.ZERO;
            for (ItemDto item : itens) {
                int unidadesGratis = item.quantidade() / 3;
                total = total.add(item.precoUnitario().multiply(BigDecimal.valueOf(unidadesGratis)));
            }
            return Dinheiro.round(total);
        }
    };

    public abstract boolean aplicavel(List<ItemDto> itens, BigDecimal subtotalProdutos);

    public abstract BigDecimal desconto(List<ItemDto> itens, BigDecimal subtotalProdutos, BigDecimal frete);
}
