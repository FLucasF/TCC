package com.loja.checkout.cupom;

import com.loja.checkout.dto.ItemRequest;
import com.loja.checkout.service.Dinheiro;

import java.math.BigDecimal;
import java.util.List;

public enum Cupom {

    BEMVINDO10 {
        @Override
        public boolean aplicavel(List<ItemRequest> itens, BigDecimal subtotalProdutos) {
            return true;
        }

        @Override
        public BigDecimal desconto(List<ItemRequest> itens, BigDecimal subtotalProdutos, BigDecimal frete) {
            return Dinheiro.arredondar(subtotalProdutos.multiply(new BigDecimal("0.10")));
        }
    },

    MENOS50 {
        @Override
        public boolean aplicavel(List<ItemRequest> itens, BigDecimal subtotalProdutos) {
            return subtotalProdutos.compareTo(new BigDecimal("300.00")) >= 0;
        }

        @Override
        public BigDecimal desconto(List<ItemRequest> itens, BigDecimal subtotalProdutos, BigDecimal frete) {
            return Dinheiro.arredondar(new BigDecimal("50.00"));
        }
    },

    FRETEGRATIS {
        @Override
        public boolean aplicavel(List<ItemRequest> itens, BigDecimal subtotalProdutos) {
            return true;
        }

        @Override
        public BigDecimal desconto(List<ItemRequest> itens, BigDecimal subtotalProdutos, BigDecimal frete) {
            return Dinheiro.arredondar(frete);
        }
    },

    LEVE3PAGUE2 {
        @Override
        public boolean aplicavel(List<ItemRequest> itens, BigDecimal subtotalProdutos) {
            return true;
        }

        @Override
        public BigDecimal desconto(List<ItemRequest> itens, BigDecimal subtotalProdutos, BigDecimal frete) {
            BigDecimal total = BigDecimal.ZERO;
            for (ItemRequest item : itens) {
                int unidadesGratis = item.quantidade() / 3;
                total = total.add(item.precoUnitario().multiply(BigDecimal.valueOf(unidadesGratis)));
            }
            return Dinheiro.arredondar(total);
        }
    };

    public abstract boolean aplicavel(List<ItemRequest> itens, BigDecimal subtotalProdutos);

    public abstract BigDecimal desconto(List<ItemRequest> itens, BigDecimal subtotalProdutos, BigDecimal frete);
}
