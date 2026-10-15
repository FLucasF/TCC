package com.loja.checkout.enums;

import com.loja.checkout.dto.ItemCarrinho;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public enum Cupom {
    BEMVINDO10 {
        @Override
        public boolean isAplicavel(BigDecimal subtotalProdutos, List<ItemCarrinho> itens) {
            return true;
        }

        @Override
        public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete, List<ItemCarrinho> itens) {
            BigDecimal desconto = subtotalProdutos.multiply(new BigDecimal("0.10"));
            return desconto.setScale(2, RoundingMode.HALF_EVEN);
        }
    },

    MENOS50 {
        @Override
        public boolean isAplicavel(BigDecimal subtotalProdutos, List<ItemCarrinho> itens) {
            return subtotalProdutos.compareTo(new BigDecimal("300.00")) >= 0;
        }

        @Override
        public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete, List<ItemCarrinho> itens) {
            return new BigDecimal("50.00");
        }
    },

    FRETEGRATIS {
        @Override
        public boolean isAplicavel(BigDecimal subtotalProdutos, List<ItemCarrinho> itens) {
            return true;
        }

        @Override
        public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete, List<ItemCarrinho> itens) {
            return frete;
        }
    },

    LEVE3PAGUE2 {
        @Override
        public boolean isAplicavel(BigDecimal subtotalProdutos, List<ItemCarrinho> itens) {
            return true;
        }

        @Override
        public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete, List<ItemCarrinho> itens) {
            BigDecimal descontoTotal = BigDecimal.ZERO;

            for (ItemCarrinho item : itens) {
                int quantidade = item.getQuantidade();
                int unidadesGratis = quantidade / 3;

                if (unidadesGratis > 0) {
                    BigDecimal descontoItem = item.getPrecoUnitario()
                        .multiply(new BigDecimal(unidadesGratis));
                    descontoTotal = descontoTotal.add(descontoItem);
                }
            }

            return descontoTotal.setScale(2, RoundingMode.HALF_EVEN);
        }
    };

    public abstract boolean isAplicavel(BigDecimal subtotalProdutos, List<ItemCarrinho> itens);
    public abstract BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete, List<ItemCarrinho> itens);
}
