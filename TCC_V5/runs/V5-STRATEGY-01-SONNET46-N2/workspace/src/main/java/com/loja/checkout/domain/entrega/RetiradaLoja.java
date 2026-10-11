package com.loja.checkout.domain.entrega;

import java.math.BigDecimal;

public class RetiradaLoja implements ModalidadeEntrega {

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoTotal) {
        return BigDecimal.ZERO.setScale(2);
    }

    @Override
    public int prazoEntregaDias() {
        return 1;
    }

    @Override
    public void validarDisponibilidade(BigDecimal pesoTotal) {
        // sem restrição
    }
}
