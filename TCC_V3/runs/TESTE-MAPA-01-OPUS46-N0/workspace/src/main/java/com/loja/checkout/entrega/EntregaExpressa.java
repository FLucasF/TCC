package com.loja.checkout.entrega;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

@Component
public class EntregaExpressa implements CalculoEntrega {

    @Override
    public String codigo() {
        return "EXPRESSA";
    }

    @Override
    public ResultadoEntrega calcular(BigDecimal pesoKg) {
        BigDecimal frete = new BigDecimal("25.00")
                .add(new BigDecimal("4.50").multiply(pesoKg))
                .setScale(2, RoundingMode.HALF_EVEN);
        return new ResultadoEntrega(frete, 2);
    }
}
