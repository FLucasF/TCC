package com.loja.checkout.entrega;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

@Component
public class EntregaEconomica implements CalculoEntrega {

    @Override
    public String codigo() {
        return "ECONOMICA";
    }

    @Override
    public ResultadoEntrega calcular(BigDecimal pesoKg) {
        BigDecimal frete = new BigDecimal("12.00")
                .add(new BigDecimal("2.00").multiply(pesoKg))
                .setScale(2, RoundingMode.HALF_EVEN);
        return new ResultadoEntrega(frete, 7);
    }
}
