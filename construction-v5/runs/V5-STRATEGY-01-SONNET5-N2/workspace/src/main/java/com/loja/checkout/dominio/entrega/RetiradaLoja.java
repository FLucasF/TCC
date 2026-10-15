package com.loja.checkout.dominio.entrega;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class RetiradaLoja implements ModalidadeEntrega {

    @Override
    public String codigo() {
        return "RETIRADA_LOJA";
    }

    @Override
    public boolean disponivel(double pesoTotalKg) {
        return true;
    }

    @Override
    public BigDecimal calcularFrete(double pesoTotalKg) {
        return BigDecimal.ZERO.setScale(2);
    }

    @Override
    public int prazoEntregaDias() {
        return 1;
    }
}
