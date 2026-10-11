package com.loja.checkout.domain.entrega;

import java.math.BigDecimal;

class Motoboy implements ModalidadeEntrega {

    private static final BigDecimal FRETE = new BigDecimal("18.00");
    private static final BigDecimal PESO_MAXIMO = new BigDecimal("5");

    @Override
    public String codigo() { return "MOTOBOY"; }

    @Override
    public boolean disponivel(BigDecimal pesoKg) {
        return pesoKg.compareTo(PESO_MAXIMO) <= 0;
    }

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoKg) { return FRETE; }

    @Override
    public int prazoDias() { return 0; }
}
