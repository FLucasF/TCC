package com.loja.checkout.delivery;

import com.loja.checkout.util.Dinheiro;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class Economica implements DeliveryOption {

    private static final BigDecimal TAXA_FIXA = new BigDecimal("12.00");
    private static final BigDecimal TAXA_POR_KG = new BigDecimal("2.00");

    @Override
    public String codigo() {
        return "ECONOMICA";
    }

    @Override
    public boolean disponivel(BigDecimal pesoKg) {
        return true;
    }

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoKg) {
        return Dinheiro.arredondar(TAXA_FIXA.add(TAXA_POR_KG.multiply(pesoKg)));
    }

    @Override
    public int prazoDias() {
        return 7;
    }
}
