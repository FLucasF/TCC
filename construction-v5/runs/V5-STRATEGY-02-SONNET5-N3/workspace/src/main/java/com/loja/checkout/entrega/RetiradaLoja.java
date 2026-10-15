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
    public boolean disponivel(BigDecimal pesoKg) {
        return true;
    }

    @Override
    public BigDecimal custo(BigDecimal pesoKg) {
        return BigDecimal.ZERO;
    }

    @Override
    public int prazoDias() {
        return 1;
    }
}
