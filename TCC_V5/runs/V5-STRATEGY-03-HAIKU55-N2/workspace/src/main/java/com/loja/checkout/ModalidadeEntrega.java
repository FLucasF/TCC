package com.loja.checkout;

import java.math.BigDecimal;

public enum ModalidadeEntrega {

    ECONOMICA(7) {
        @Override
        BigDecimal frete(Carrinho carrinho) {
            return new BigDecimal("12.00").add(new BigDecimal("2.00").multiply(carrinho.pesoKg()));
        }
    },
    EXPRESSA(2) {
        @Override
        BigDecimal frete(Carrinho carrinho) {
            return new BigDecimal("25.00").add(new BigDecimal("4.50").multiply(carrinho.pesoKg()));
        }
    },
    RETIRADA_LOJA(1) {
        @Override
        BigDecimal frete(Carrinho carrinho) {
            return BigDecimal.ZERO;
        }
    },
    MOTOBOY(0) {
        @Override
        BigDecimal frete(Carrinho carrinho) {
            return new BigDecimal("18.00");
        }

        @Override
        boolean disponivel(Carrinho carrinho) {
            return carrinho.pesoKg().compareTo(new BigDecimal("5")) <= 0;
        }
    };

    private final int prazoDias;

    ModalidadeEntrega(int prazoDias) {
        this.prazoDias = prazoDias;
    }

    int prazoDias() {
        return prazoDias;
    }

    abstract BigDecimal frete(Carrinho carrinho);

    boolean disponivel(Carrinho carrinho) {
        return true;
    }

    static ModalidadeEntrega de(String codigo) {
        return ResumoException.paraEnum(values(), codigo, "MODALIDADE_INVALIDA");
    }
}
