package com.loja.checkout.dominio;

import java.math.BigDecimal;

public enum ModalidadeEntrega {

    ECONOMICA(new BigDecimal("12.00"), new BigDecimal("2.00"), 7),
    EXPRESSA(new BigDecimal("25.00"), new BigDecimal("4.50"), 2),
    RETIRADA_LOJA(Dinheiro.ZERO, Dinheiro.ZERO, 1),
    MOTOBOY(new BigDecimal("18.00"), Dinheiro.ZERO, 0) {
        @Override
        public void validarPara(Pedido pedido) {
            if (pedido.pesoKg().compareTo(PESO_MAXIMO_KG) > 0) {
                throw new RecusaPedido(Erro.MODALIDADE_INDISPONIVEL);
            }
        }
    };

    private static final BigDecimal PESO_MAXIMO_KG = new BigDecimal("5");

    private final BigDecimal taxaFixa;
    private final BigDecimal taxaPorKg;
    private final int prazoDias;

    ModalidadeEntrega(BigDecimal taxaFixa, BigDecimal taxaPorKg, int prazoDias) {
        this.taxaFixa = taxaFixa;
        this.taxaPorKg = taxaPorKg;
        this.prazoDias = prazoDias;
    }

    public void validarPara(Pedido pedido) {
    }

    public BigDecimal frete(Pedido pedido) {
        return Dinheiro.centavos(taxaFixa.add(taxaPorKg.multiply(pedido.pesoKg())));
    }

    public int prazoDias() {
        return prazoDias;
    }
}
