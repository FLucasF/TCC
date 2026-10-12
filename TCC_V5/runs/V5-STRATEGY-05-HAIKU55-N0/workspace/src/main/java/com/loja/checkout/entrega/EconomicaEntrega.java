package com.loja.checkout.entrega;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import com.loja.checkout.calculo.Dinheiro;

@Component
public class EconomicaEntrega implements ModalidadeEntrega {

    @Override
    public String codigo() {
        return "ECONOMICA";
    }

    @Override
    public boolean atende(BigDecimal pesoKg) {
        return true;
    }

    @Override
    public BigDecimal frete(BigDecimal pesoKg) {
        return Dinheiro.arredondar(new BigDecimal("12.00").add(new BigDecimal("2.00").multiply(pesoKg)));
    }

    @Override
    public int prazoDias() {
        return 7;
    }
}
