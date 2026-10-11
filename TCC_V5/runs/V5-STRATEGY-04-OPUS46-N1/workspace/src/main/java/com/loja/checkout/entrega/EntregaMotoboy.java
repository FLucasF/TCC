package com.loja.checkout.entrega;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class EntregaMotoboy implements CalculoEntrega {

    private static final BigDecimal LIMITE_PESO = new BigDecimal("5");
    private static final BigDecimal VALOR_FIXO = new BigDecimal("18.00");

    @Override
    public String getCodigo() {
        return "MOTOBOY";
    }

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoTotalKg) {
        return VALOR_FIXO;
    }

    @Override
    public int getPrazoDias() {
        return 0;
    }

    @Override
    public boolean isDisponivel(BigDecimal pesoTotalKg) {
        return pesoTotalKg.compareTo(LIMITE_PESO) <= 0;
    }
}
