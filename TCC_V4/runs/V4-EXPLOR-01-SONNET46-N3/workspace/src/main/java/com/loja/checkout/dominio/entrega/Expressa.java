package com.loja.checkout.dominio.entrega;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Expressa implements ModalidadeEntrega {

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoKg) {
        return new BigDecimal("25.00")
                .add(new BigDecimal("4.50").multiply(pesoKg))
                .setScale(2, RoundingMode.HALF_EVEN);
    }

    @Override
    public int prazoEntregaDias() {
        return 2;
    }

    @Override
    public void validarDisponibilidade(BigDecimal pesoKg) {
        // sem restrições
    }
}
