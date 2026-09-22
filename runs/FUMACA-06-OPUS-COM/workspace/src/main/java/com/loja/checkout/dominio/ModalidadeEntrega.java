package com.loja.checkout.dominio;

import java.math.BigDecimal;

/**
 * Cada modalidade cobra o frete como uma parte fixa mais uma parte por quilo,
 * tem seu prazo e decide se atende o pedido.
 */
public enum ModalidadeEntrega {

    ECONOMICA("12.00", "2.00", 7),

    EXPRESSA("25.00", "4.50", 2),

    RETIRADA_LOJA("0.00", "0.00", 1),

    MOTOBOY("18.00", "0.00", 0) {
        private static final BigDecimal PESO_MAXIMO_KG = new BigDecimal("5");

        @Override
        public boolean atende(Pedido pedido) {
            return pedido.pesoKg().compareTo(PESO_MAXIMO_KG) <= 0;
        }
    };

    private final BigDecimal parteFixa;
    private final BigDecimal porKg;
    private final int prazoDias;

    ModalidadeEntrega(String parteFixa, String porKg, int prazoDias) {
        this.parteFixa = new BigDecimal(parteFixa);
        this.porKg = new BigDecimal(porKg);
        this.prazoDias = prazoDias;
    }

    public static ModalidadeEntrega de(String codigo) {
        return Catalogo.resolver(ModalidadeEntrega.class, codigo, CodigoErro.MODALIDADE_INVALIDA);
    }

    public BigDecimal frete(Pedido pedido) {
        return Dinheiro.emCentavos(parteFixa.add(porKg.multiply(pedido.pesoKg())));
    }

    public int prazoDias() {
        return prazoDias;
    }

    public boolean atende(Pedido pedido) {
        return true;
    }
}
