package br.com.loja.checkout.clube;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

@Component
public class Prata implements NivelClube {

    @Override
    public String codigo() {
        return "PRATA";
    }

    @Override
    public BigDecimal freteCobrado(BigDecimal frete) {
        return frete;
    }

    @Override
    public BigDecimal credito(BigDecimal subtotalProdutos) {
        return subtotalProdutos.multiply(new BigDecimal("0.02"));
    }

    @Override
    public boolean brinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
