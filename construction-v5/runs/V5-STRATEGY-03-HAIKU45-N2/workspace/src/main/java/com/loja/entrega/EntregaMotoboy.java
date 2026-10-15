package com.loja.entrega;

import java.math.BigDecimal;

public class EntregaMotoboy implements Entrega {
    private static final double PESO_MAXIMO = 5.0;

    @Override
    public BigDecimal calcularFrete(double pesoTotal) {
        return new BigDecimal("18.00");
    }

    @Override
    public Integer obterPrazo() {
        return 0;
    }

    @Override
    public boolean estaDisponivel(double pesoTotal) {
        return pesoTotal <= PESO_MAXIMO;
    }
}
