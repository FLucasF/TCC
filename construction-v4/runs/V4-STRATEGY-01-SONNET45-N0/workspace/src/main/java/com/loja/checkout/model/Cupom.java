package com.loja.checkout.model;

import com.loja.checkout.dto.ItemRequest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum Cupom {
    BEMVINDO10 {
        @Override
        public boolean aplicavel(BigDecimal subtotalProdutos, List<ItemRequest> itens) {
            return true;
        }

        @Override
        public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete, List<ItemRequest> itens) {
            return subtotalProdutos.multiply(new BigDecimal("0.10"))
                    .setScale(2, RoundingMode.HALF_EVEN);
        }
    },
    MENOS50 {
        @Override
        public boolean aplicavel(BigDecimal subtotalProdutos, List<ItemRequest> itens) {
            return subtotalProdutos.compareTo(new BigDecimal("300.00")) >= 0;
        }

        @Override
        public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete, List<ItemRequest> itens) {
            return new BigDecimal("50.00");
        }
    },
    FRETEGRATIS {
        @Override
        public boolean aplicavel(BigDecimal subtotalProdutos, List<ItemRequest> itens) {
            return true;
        }

        @Override
        public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete, List<ItemRequest> itens) {
            return frete;
        }
    },
    LEVE3PAGUE2 {
        @Override
        public boolean aplicavel(BigDecimal subtotalProdutos, List<ItemRequest> itens) {
            return true;
        }

        @Override
        public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete, List<ItemRequest> itens) {
            Map<String, ItemRequest> itensPorNome = itens.stream()
                    .collect(Collectors.toMap(ItemRequest::getNome, Function.identity()));

            BigDecimal desconto = BigDecimal.ZERO;
            for (ItemRequest item : itensPorNome.values()) {
                int unidadesGratis = item.getQuantidade() / 3;
                BigDecimal descontoItem = item.getPrecoUnitario()
                        .multiply(BigDecimal.valueOf(unidadesGratis))
                        .setScale(2, RoundingMode.HALF_EVEN);
                desconto = desconto.add(descontoItem);
            }
            return desconto;
        }
    };

    public abstract boolean aplicavel(BigDecimal subtotalProdutos, List<ItemRequest> itens);
    public abstract BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete, List<ItemRequest> itens);
}
