package com.loja.checkout.entrega;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import com.loja.checkout.calculo.Dinheiro;

@Component
public class ExpressaEntrega implements ModalidadeEntrega {

    @Override
    public String codigo() {
        return "EXPRESSA";
    }

    @Override
    public boolean atende(BigDecimal pesoKg) {
        return true;
    }

    @Override
    public BigDecimal frete(BigDecimal pesoKg) {
        return Dinheiro.arredondar(new BigDecimal("25.00").add(new BigDecimal("4.50").multiply(pesoKg)));
    }

    @Override
    public int prazoDias() {
        return 2;
    }
}
