package com.loja.checkout.service.entrega;

import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
public class EntregaMotoboy implements EstrategiaEntrega {

    private static final BigDecimal PESO_MAXIMO = new BigDecimal("5");

    @Override
    public String getCodigo() {
        return "MOTOBOY";
    }

    @Override
    public boolean atendePedido(BigDecimal pesoTotalKg) {
        return pesoTotalKg.compareTo(PESO_MAXIMO) <= 0;
    }

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoTotalKg) {
        return new BigDecimal("18.00");
    }

    @Override
    public int prazoEmDias() {
        return 0;
    }
}
