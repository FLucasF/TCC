package com.loja.checkout.dominio.entrega;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class EntregaExpressa implements ModalidadeEntrega {

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoKgTotal) {
        return new BigDecimal("25.00")
                .add(new BigDecimal("4.50").multiply(pesoKgTotal))
                .setScale(2, RoundingMode.HALF_EVEN);
    }

    @Override
    public int prazoEntregaDias() {
        return 2;
    }

    @Override
    public boolean estaDisponivel(BigDecimal pesoKgTotal) {
        return true;
    }
}
