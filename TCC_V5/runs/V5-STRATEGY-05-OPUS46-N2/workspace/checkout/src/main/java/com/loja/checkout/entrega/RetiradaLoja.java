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
    public BigDecimal calcularFrete(BigDecimal pesoTotalKg) {
        return BigDecimal.ZERO.setScale(2);
    }

    @Override
    public int prazoDias() {
        return 1;
    }

    @Override
    public boolean disponivel(BigDecimal pesoTotalKg) {
        return true;
    }
}
