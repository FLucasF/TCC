package com.loja.checkout.entrega;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class EntregaExpressa implements ModalidadeEntrega {

    @Override
    public String getCodigo() {
        return "EXPRESSA";
    }

    @Override
    public BigDecimal calcularFrete(double pesoKg) {
        return new BigDecimal("25.00")
                .add(new BigDecimal("4.50").multiply(BigDecimal.valueOf(pesoKg)))
                .setScale(2, RoundingMode.HALF_EVEN);
    }

    @Override
    public int getPrazoEntregaDias() {
        return 2;
    }

    @Override
    public void verificarDisponibilidade(double pesoKg) {
    }
}
