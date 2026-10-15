package com.loja.checkout.entrega;

import java.math.BigDecimal;

public class EntregaRetiradaLoja implements ModalidadeEntrega {

    @Override
    public BigDecimal calcularFrete(double pesoKg) {
        return BigDecimal.ZERO.setScale(2);
    }

    @Override
    public int getPrazo() {
        return 1;
    }

    @Override
    public void validarDisponibilidade(double pesoKg) {
    }
}
