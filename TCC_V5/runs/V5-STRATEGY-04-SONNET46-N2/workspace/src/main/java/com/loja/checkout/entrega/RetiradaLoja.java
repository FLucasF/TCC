package com.loja.checkout.entrega;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class RetiradaLoja implements ModalidadeEntrega {

    @Override
    public String getCodigo() {
        return "RETIRADA_LOJA";
    }

    @Override
    public BigDecimal calcularFrete(double pesoKg) {
        return BigDecimal.ZERO.setScale(2);
    }

    @Override
    public int getPrazoEntregaDias() {
        return 1;
    }

    @Override
    public void verificarDisponibilidade(double pesoKg) {
    }
}
