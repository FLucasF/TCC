package com.loja.checkout.dominio.entrega;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Economica implements ModalidadeEntrega {

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
    public void validarDisponibilidade(BigDecimal pesoKg) {
        // sem restrições
    }
}
