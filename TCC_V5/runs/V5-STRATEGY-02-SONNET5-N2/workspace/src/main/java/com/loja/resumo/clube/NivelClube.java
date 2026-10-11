package com.loja.resumo.clube;

import com.loja.resumo.dinheiro.Dinheiro;

import java.math.BigDecimal;
import java.util.Optional;

public enum NivelClube {

    BRONZE {
        @Override
        public BigDecimal credito(BigDecimal subtotalProdutos) {
            return BigDecimal.ZERO.setScale(2);
        }

        @Override
        public boolean freteGratis() {
            return false;
        }

        @Override
        public boolean temBrinde(BigDecimal subtotalProdutos) {
            return false;
        }
    },

    PRATA {
        @Override
        public BigDecimal credito(BigDecimal subtotalProdutos) {
            return Dinheiro.arredondar(subtotalProdutos.multiply(new BigDecimal("0.02")));
        }

        @Override
        public boolean freteGratis() {
            return false;
        }

        @Override
        public boolean temBrinde(BigDecimal subtotalProdutos) {
            return false;
        }
    },

    OURO {
        @Override
        public BigDecimal credito(BigDecimal subtotalProdutos) {
            return Dinheiro.arredondar(subtotalProdutos.multiply(new BigDecimal("0.05")));
        }

        @Override
        public boolean freteGratis() {
            return true;
        }

        @Override
        public boolean temBrinde(BigDecimal subtotalProdutos) {
            return subtotalProdutos.compareTo(LIMITE_BRINDE_OURO) > 0;
        }
    };

    private static final BigDecimal LIMITE_BRINDE_OURO = new BigDecimal("500.00");

    public abstract BigDecimal credito(BigDecimal subtotalProdutos);

    public abstract boolean freteGratis();

    public abstract boolean temBrinde(BigDecimal subtotalProdutos);

    public static Optional<NivelClube> encontrar(String nome) {
        if (nome == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(NivelClube.valueOf(nome));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
