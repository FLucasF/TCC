package com.loja.checkout.entrega;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class EntregaEconomica implements ModalidadeEntrega {

    @Override
    public String getCodigo() {
        return "ECONOMICA";
    }

    @Override
    public BigDecimal calcularFrete(double pesoKg) {
        return new BigDecimal("12.00")
                .add(new BigDecimal("2.00").multiply(BigDecimal.valueOf(pesoKg)))
                .setScale(2, RoundingMode.HALF_EVEN);
    }

    @Override
    public int getPrazoEntregaDias() {
        return 7;
    }

    @Override
    public void verificarDisponibilidade(double pesoKg) {
    }
}
