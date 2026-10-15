package com.loja.checkout.entrega;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class EntregaMotoboy implements ModalidadeEntrega {

    private static final BigDecimal LIMITE_PESO = new BigDecimal("5");

    @Override
    public String codigo() {
        return "MOTOBOY";
    }

    @Override
    public boolean aceita(BigDecimal pesoKgTotal) {
        return pesoKgTotal.compareTo(LIMITE_PESO) <= 0;
    }

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoKgTotal) {
        return new BigDecimal("18.00");
    }

    @Override
    public int prazoEntregaDias() {
        return 0;
    }
}
