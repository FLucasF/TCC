package com.loja.checkout.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public enum Cupom {
    BEMVINDO10 {
        @Override
        public Optional<String> validar(BigDecimal subtotalProdutos) {
            return Optional.empty();
        }

        @Override
        public BigDecimal calcular(BigDecimal subtotalProdutos, List<?> itens) {
            return Arredonda.round(subtotalProdutos.multiply(new BigDecimal("0.10")));
        }
    },
    MENOS50 {
        @Override
        public Optional<String> validar(BigDecimal subtotalProdutos) {
            if (subtotalProdutos.compareTo(new BigDecimal("300.00")) < 0) {
                return Optional.of("CUPOM_NAO_APLICAVEL");
            }
            return Optional.empty();
        }

        @Override
        public BigDecimal calcular(BigDecimal subtotalProdutos, List<?> itens) {
            return new BigDecimal("50.00");
        }
    },
    FRETEGRATIS {
        @Override
        public Optional<String> validar(BigDecimal subtotalProdutos) {
            return Optional.empty();
        }

        @Override
        public BigDecimal calcular(BigDecimal subtotalProdutos, List<?> itens) {
            return BigDecimal.ZERO;
        }

        @Override
        public boolean ehFretegratis() {
            return true;
        }
    },
    LEVE3PAGUE2 {
        @Override
        public Optional<String> validar(BigDecimal subtotalProdutos) {
            return Optional.empty();
        }

        @Override
        public BigDecimal calcular(BigDecimal subtotalProdutos, List<?> itens) {
            BigDecimal desconto = BigDecimal.ZERO;
            for (Object item : itens) {
                Item it = (Item) item;
                int quantidade = it.quantidade();
                int gratis = quantidade / 3;
                BigDecimal valorGratis = it.precoUnitario().multiply(new BigDecimal(gratis));
                desconto = desconto.add(valorGratis);
            }
            return Arredonda.round(desconto);
        }
    };

    public abstract Optional<String> validar(BigDecimal subtotalProdutos);

    public abstract BigDecimal calcular(BigDecimal subtotalProdutos, List<?> itens);

    public boolean ehFretegratis() {
        return false;
    }

    public static Optional<Cupom> buscar(String codigo) {
        try {
            return Optional.of(Cupom.valueOf(codigo));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
