package com.loja.checkout.delivery;

import com.loja.checkout.util.Money;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class EconomicaDeliveryMethod implements DeliveryMethod {

    private static final BigDecimal TAXA_FIXA = new BigDecimal("12.00");
    private static final BigDecimal TAXA_POR_KG = new BigDecimal("2.00");

    @Override
    public String getCodigo() {
        return "ECONOMICA";
    }

    @Override
    public int getPrazoDias() {
        return 7;
    }

    @Override
    public boolean isDisponivel(BigDecimal pesoTotalKg) {
        return true;
    }

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoTotalKg) {
        return Money.round(TAXA_FIXA.add(TAXA_POR_KG.multiply(pesoTotalKg)));
    }
}
