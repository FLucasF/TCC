package com.loja.checkout.delivery;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class Motoboy implements DeliveryOption {

    private static final BigDecimal TAXA_FIXA = new BigDecimal("18.00");
    private static final BigDecimal PESO_MAXIMO_KG = new BigDecimal("5");

    @Override
    public String codigo() {
        return "MOTOBOY";
    }

    @Override
    public boolean disponivel(BigDecimal pesoKg) {
        return pesoKg.compareTo(PESO_MAXIMO_KG) <= 0;
    }

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoKg) {
        return TAXA_FIXA;
    }

    @Override
    public int prazoDias() {
        return 0;
    }
}
