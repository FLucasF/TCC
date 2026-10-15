package br.tcc.checkout.domain;

import java.math.BigDecimal;
import java.util.List;
import br.tcc.checkout.dto.Item;

public enum Cupom {
    BEMVINDO10 {
        @Override
        public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, List<Item> itens) {
            return subtotalProdutos.multiply(BigDecimal.valueOf(0.10));
        }

        @Override
        public boolean estaAplicavel(BigDecimal subtotalProdutos, List<Item> itens) {
            return true;
        }
    },
    MENOS50 {
        @Override
        public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, List<Item> itens) {
            return BigDecimal.valueOf(50.0);
        }

        @Override
        public boolean estaAplicavel(BigDecimal subtotalProdutos, List<Item> itens) {
            return subtotalProdutos.compareTo(BigDecimal.valueOf(300.0)) >= 0;
        }
    },
    FRETEGRATIS {
        @Override
        public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, List<Item> itens) {
            return BigDecimal.ZERO;
        }

        @Override
        public boolean estaAplicavel(BigDecimal subtotalProdutos, List<Item> itens) {
            return true;
        }
    },
    LEVE3PAGUE2 {
        @Override
        public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, List<Item> itens) {
            BigDecimal desconto = BigDecimal.ZERO;
            for (Item item : itens) {
                int quantidade = item.quantidade();
                int itensGratis = quantidade / 3;
                BigDecimal precoDesconto = item.precoUnitario().multiply(BigDecimal.valueOf(itensGratis));
                desconto = desconto.add(precoDesconto);
            }
            return desconto;
        }

        @Override
        public boolean estaAplicavel(BigDecimal subtotalProdutos, List<Item> itens) {
            return true;
        }
    };

    public abstract BigDecimal calcularDesconto(BigDecimal subtotalProdutos, List<Item> itens);

    public abstract boolean estaAplicavel(BigDecimal subtotalProdutos, List<Item> itens);

    public static Cupom fromString(String codigo) {
        if (codigo == null || codigo.isEmpty()) {
            return null;
        }
        try {
            return Cupom.valueOf(codigo);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public boolean isFretegratis() {
        return this == FRETEGRATIS;
    }
}
