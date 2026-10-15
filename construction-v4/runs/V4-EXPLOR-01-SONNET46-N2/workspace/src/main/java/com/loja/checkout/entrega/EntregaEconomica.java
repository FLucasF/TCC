package com.loja.checkout.entrega;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class EntregaEconomica implements ModalidadeEntrega {

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoKg) {
        return new BigDecimal("12.00")
                .add(new BigDecimal("2.00").multiply(pesoKg))
                .setScale(2, RoundingMode.HALF_EVEN);
    }

    @Override
    public int prazoEntregaDias() {
        return 7;
    }

    @Override
    public boolean aceitaPedido(BigDecimal pesoKg) {
        return true;
    }
}
