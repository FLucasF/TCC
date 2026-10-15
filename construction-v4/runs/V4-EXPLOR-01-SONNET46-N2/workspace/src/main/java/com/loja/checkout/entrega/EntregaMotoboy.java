package com.loja.checkout.entrega;

import java.math.BigDecimal;

public class EntregaMotoboy implements ModalidadeEntrega {

    private static final BigDecimal LIMITE_PESO = new BigDecimal("5");

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoKg) {
        return new BigDecimal("18.00");
    }

    @Override
    public int prazoEntregaDias() {
        return 0;
    }

    @Override
    public boolean aceitaPedido(BigDecimal pesoKg) {
        return pesoKg.compareTo(LIMITE_PESO) <= 0;
    }
}
