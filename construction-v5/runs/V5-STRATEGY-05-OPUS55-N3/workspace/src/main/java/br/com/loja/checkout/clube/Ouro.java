package br.com.loja.checkout.clube;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

@Component
public class Ouro implements NivelClube {

    @Override
    public String codigo() {
        return "OURO";
    }

    @Override
    public BigDecimal freteCobrado(BigDecimal frete) {
        return BigDecimal.ZERO;
    }

    @Override
    public BigDecimal credito(BigDecimal subtotalProdutos) {
        return subtotalProdutos.multiply(new BigDecimal("0.05"));
    }

    @Override
    public boolean brinde(BigDecimal subtotalProdutos) {
        return subtotalProdutos.compareTo(new BigDecimal("500.00")) > 0;
    }
}
