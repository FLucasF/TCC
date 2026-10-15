package com.loja.checkout.entrega;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class EntregaEconomica implements ModalidadeEntrega {

    @Override
    public String codigo() {
        return "ECONOMICA";
    }

    @Override
    public boolean aceita(BigDecimal pesoKgTotal) {
        return true;
    }

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoKgTotal) {
        return new BigDecimal("12.00")
                .add(new BigDecimal("2.00").multiply(pesoKgTotal))
                .setScale(2, RoundingMode.HALF_EVEN);
    }

    @Override
    public int prazoEntregaDias() {
        return 7;
    }
}
