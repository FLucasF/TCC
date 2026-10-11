package com.loja.checkout.entrega;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class Motoboy implements ModalidadeEntrega {

    private static final BigDecimal PESO_MAXIMO = new BigDecimal("5");

    @Override
    public String codigo() {
        return "MOTOBOY";
    }

    @Override
    public boolean disponivel(BigDecimal pesoTotalKg) {
        return pesoTotalKg.compareTo(PESO_MAXIMO) <= 0;
    }

    @Override
    public ResultadoEntrega calcular(BigDecimal pesoTotalKg) {
        return new ResultadoEntrega(new BigDecimal("18.00"), 0);
    }
}
