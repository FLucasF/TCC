package com.loja.checkout.domain.entrega;

import java.math.BigDecimal;

public class Motoboy implements ModalidadeEntrega {
    private static final BigDecimal TAXA = new BigDecimal("18.00");
    private static final BigDecimal PESO_MAXIMO = new BigDecimal("5.00");

    @Override
    public boolean estaDisponivel(BigDecimal pesoKg) {
        return pesoKg.compareTo(PESO_MAXIMO) <= 0;
    }

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoKg) {
        return TAXA;
    }

    @Override
    public int obterPrazo() {
        return 0;
    }
}
