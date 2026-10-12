package com.loja.checkout.entrega;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class EntregaRetiradaLoja implements ModalidadeEntrega {

    @Override
    public String codigo() {
        return "RETIRADA_LOJA";
    }

    @Override
    public boolean aceita(BigDecimal pesoKgTotal) {
        return true;
    }

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoKgTotal) {
        return BigDecimal.ZERO.setScale(2);
    }

    @Override
    public int prazoEntregaDias() {
        return 1;
    }
}
