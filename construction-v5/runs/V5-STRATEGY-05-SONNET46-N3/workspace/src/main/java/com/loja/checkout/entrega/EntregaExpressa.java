package com.loja.checkout.entrega;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class EntregaExpressa implements ModalidadeEntrega {

    @Override
    public BigDecimal calcularFrete(double pesoKg) {
        return BigDecimal.valueOf(25.00 + 4.50 * pesoKg).setScale(2, RoundingMode.HALF_EVEN);
    }

    @Override
    public int getPrazo() {
        return 2;
    }

    @Override
    public void validarDisponibilidade(double pesoKg) {
    }
}
