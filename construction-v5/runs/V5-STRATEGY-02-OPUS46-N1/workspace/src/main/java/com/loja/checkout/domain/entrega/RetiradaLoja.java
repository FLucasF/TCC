package com.loja.checkout.domain.entrega;

import java.math.BigDecimal;

public class RetiradaLoja implements ModalidadeEntrega {

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoTotalKg) {
        return BigDecimal.ZERO.setScale(2);
    }

    @Override
    public int getPrazoDias() {
        return 1;
    }

    @Override
    public boolean isDisponivel(BigDecimal pesoTotalKg) {
        return true;
    }

    @Override
    public String getCodigo() {
        return "RETIRADA_LOJA";
    }
}
