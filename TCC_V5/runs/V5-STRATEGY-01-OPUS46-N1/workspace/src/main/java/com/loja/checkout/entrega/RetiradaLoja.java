package com.loja.checkout.entrega;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class RetiradaLoja implements ModalidadeEntrega {

    @Override
    public String codigo() {
        return "RETIRADA_LOJA";
    }

    @Override
    public boolean disponivel(BigDecimal pesoTotalKg) {
        return true;
    }

    @Override
    public ResultadoEntrega calcular(BigDecimal pesoTotalKg) {
        return new ResultadoEntrega(BigDecimal.ZERO.setScale(2), 1);
    }
}
