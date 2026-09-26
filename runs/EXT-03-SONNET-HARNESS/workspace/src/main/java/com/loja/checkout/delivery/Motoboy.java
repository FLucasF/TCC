package com.loja.checkout.delivery;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class Motoboy implements DeliveryMethod {

    private static final BigDecimal CUSTO_FIXO = new BigDecimal("18.00");
    private static final BigDecimal PESO_MAXIMO_KG = new BigDecimal("5");

    @Override
    public String getCodigo() {
        return "MOTOBOY";
    }

    @Override
    public boolean isDisponivel(BigDecimal pesoTotalKg) {
        return pesoTotalKg.compareTo(PESO_MAXIMO_KG) <= 0;
    }

    @Override
    public BigDecimal calcularCusto(BigDecimal pesoTotalKg) {
        return CUSTO_FIXO;
    }

    @Override
    public int getPrazoDias() {
        return 0;
    }
}
