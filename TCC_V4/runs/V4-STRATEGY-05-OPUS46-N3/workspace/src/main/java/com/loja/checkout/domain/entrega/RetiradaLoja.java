package com.loja.checkout.domain.entrega;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class RetiradaLoja implements ModalidadeEntrega {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);

    @Override
    public String codigo() {
        return "RETIRADA_LOJA";
    }

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoTotal) {
        return ZERO;
    }

    @Override
    public int prazoDias() {
        return 1;
    }

    @Override
    public boolean disponivel(BigDecimal pesoTotal) {
        return true;
    }
}
