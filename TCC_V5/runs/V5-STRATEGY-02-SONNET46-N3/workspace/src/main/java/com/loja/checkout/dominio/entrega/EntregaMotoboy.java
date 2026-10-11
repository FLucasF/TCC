package com.loja.checkout.dominio.entrega;

import java.math.BigDecimal;

public class EntregaMotoboy implements ModalidadeEntrega {

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoKgTotal) {
        return new BigDecimal("18.00");
    }

    @Override
    public int prazoEntregaDias() {
        return 0;
    }

    @Override
    public boolean estaDisponivel(BigDecimal pesoKgTotal) {
        return pesoKgTotal.compareTo(new BigDecimal("5")) <= 0;
    }
}
