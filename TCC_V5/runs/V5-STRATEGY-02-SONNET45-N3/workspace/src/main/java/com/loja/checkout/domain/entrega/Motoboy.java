package com.loja.checkout.domain.entrega;

import java.math.BigDecimal;

public class Motoboy implements ModalidadeEntrega {

    private static final BigDecimal PESO_MAXIMO = new BigDecimal("5");

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoTotal) {
        return new BigDecimal("18.00");
    }

    @Override
    public int obterPrazo() {
        return 0;
    }

    @Override
    public boolean estaDisponivel(BigDecimal pesoTotal) {
        return pesoTotal.compareTo(PESO_MAXIMO) <= 0;
    }
}
