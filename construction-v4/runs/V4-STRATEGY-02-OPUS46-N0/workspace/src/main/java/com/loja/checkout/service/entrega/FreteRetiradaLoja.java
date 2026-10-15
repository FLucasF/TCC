package com.loja.checkout.service.entrega;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class FreteRetiradaLoja implements CalculadoraFrete {

    @Override
    public String codigo() {
        return "RETIRADA_LOJA";
    }

    @Override
    public boolean disponivel(BigDecimal pesoTotal) {
        return true;
    }

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoTotal) {
        return BigDecimal.ZERO.setScale(2);
    }

    @Override
    public int prazoEntregaDias() {
        return 1;
    }
}
