package com.loja.checkout.delivery;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class MotoboyDeliveryMethod implements DeliveryMethod {

    private static final BigDecimal TAXA_FIXA = new BigDecimal("18.00");
    private static final BigDecimal PESO_MAXIMO_KG = new BigDecimal("5");

    @Override
    public String getCodigo() {
        return "MOTOBOY";
    }

    @Override
    public int getPrazoDias() {
        return 0;
    }

    @Override
    public boolean isDisponivel(BigDecimal pesoTotalKg) {
        return pesoTotalKg.compareTo(PESO_MAXIMO_KG) <= 0;
    }

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoTotalKg) {
        return TAXA_FIXA;
    }
}
