package com.loja.checkout.entrega;

import java.math.BigDecimal;

/** R$ 18,00 no mesmo dia, so para pedidos de ate 5 kg. */
public final class Motoboy implements ModalidadeEntrega {

    private static final BigDecimal PESO_MAXIMO_KG = new BigDecimal("5");
    private static final BigDecimal VALOR = new BigDecimal("18.00");

    @Override
    public String codigo() {
        return "MOTOBOY";
    }

    @Override
    public boolean atende(BigDecimal pesoKg) {
        return pesoKg.compareTo(PESO_MAXIMO_KG) <= 0;
    }

    @Override
    public BigDecimal frete(BigDecimal pesoKg) {
        return VALOR;
    }

    @Override
    public int prazoDias() {
        return 0;
    }
}
