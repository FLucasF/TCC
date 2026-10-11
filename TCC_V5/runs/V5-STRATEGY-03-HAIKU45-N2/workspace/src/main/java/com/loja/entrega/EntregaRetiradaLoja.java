package com.loja.entrega;

import java.math.BigDecimal;

public class EntregaRetiradaLoja implements Entrega {
    @Override
    public BigDecimal calcularFrete(double pesoTotal) {
        return new BigDecimal("0.00");
    }

    @Override
    public Integer obterPrazo() {
        return 1;
    }

    @Override
    public boolean estaDisponivel(double pesoTotal) {
        return true;
    }
}
