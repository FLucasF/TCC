package com.loja.checkout.dominio.entrega;

import java.math.BigDecimal;

public class RetiradaLoja implements ModalidadeEntrega {

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoKg) {
        return BigDecimal.ZERO.setScale(2);
    }

    @Override
    public int prazoEntregaDias() {
        return 1;
    }

    @Override
    public void validarDisponibilidade(BigDecimal pesoKg) {
        // sem restrições
    }
}
