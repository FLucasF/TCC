package com.loja.checkout.service.entrega;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class FreteMotoboy implements CalculadoraFrete {

    private static final BigDecimal PESO_MAXIMO = new BigDecimal("5");
    private static final BigDecimal TAXA_FIXA = new BigDecimal("18.00");

    @Override
    public String codigo() {
        return "MOTOBOY";
    }

    @Override
    public boolean disponivel(BigDecimal pesoTotal) {
        return pesoTotal.compareTo(PESO_MAXIMO) <= 0;
    }

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoTotal) {
        return TAXA_FIXA;
    }

    @Override
    public int prazoEntregaDias() {
        return 0;
    }
}
