package com.loja.checkout.entrega;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class EntregaEconomica implements ModalidadeEntrega {

    @Override
    public BigDecimal calcularFrete(double pesoKg) {
        return BigDecimal.valueOf(12.00 + 2.00 * pesoKg).setScale(2, RoundingMode.HALF_EVEN);
    }

    @Override
    public int getPrazo() {
        return 7;
    }

    @Override
    public void validarDisponibilidade(double pesoKg) {
    }
}
