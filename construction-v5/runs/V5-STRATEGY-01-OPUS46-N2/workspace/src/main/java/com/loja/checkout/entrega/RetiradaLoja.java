package com.loja.checkout.entrega;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

@Component
public class RetiradaLoja implements ModalidadeEntrega {

    @Override
    public String getCodigo() {
        return "RETIRADA_LOJA";
    }

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoTotal) {
        return BigDecimal.ZERO.setScale(2);
    }

    @Override
    public int getPrazoDias() {
        return 1;
    }

    @Override
    public boolean isDisponivel(BigDecimal pesoTotal) {
        return true;
    }
}
