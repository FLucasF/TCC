package com.loja.resumo.calculo;

import java.math.BigDecimal;

public enum ModalidadeEntrega {

    ECONOMICA("12.00", "2.00", 7),
    EXPRESSA("25.00", "4.50", 2),
    RETIRADA_LOJA("0.00", "0.00", 1),
    MOTOBOY("18.00", "0.00", 0) {
        @Override
        boolean atende(Carrinho carrinho) {
            return carrinho.peso().compareTo(PESO_MAXIMO_MOTOBOY) <= 0;
        }
    };

    private static final BigDecimal PESO_MAXIMO_MOTOBOY = new BigDecimal("5");

    private final BigDecimal taxaFixa;
    private final BigDecimal porKg;
    private final int prazoDias;

    ModalidadeEntrega(String taxaFixa, String porKg, int prazoDias) {
        this.taxaFixa = new BigDecimal(taxaFixa);
        this.porKg = new BigDecimal(porKg);
        this.prazoDias = prazoDias;
    }

    BigDecimal frete(Carrinho carrinho) {
        return Dinheiro.centavos(taxaFixa.add(porKg.multiply(carrinho.peso())));
    }

    boolean atende(Carrinho carrinho) {
        return true;
    }

    public int prazoDias() {
        return prazoDias;
    }
}
