package com.loja.checkout.entrega;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class EntregaRetiradaLoja implements CalculoEntrega {

    @Override
    public String codigo() {
        return "RETIRADA_LOJA";
    }

    @Override
    public ResultadoEntrega calcular(BigDecimal pesoKg) {
        return new ResultadoEntrega(BigDecimal.ZERO.setScale(2), 1);
    }
}
