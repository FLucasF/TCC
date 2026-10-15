package br.com.loja.checkout.clube;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

@Component
public class Bronze implements NivelClube {

    @Override
    public String codigo() {
        return "BRONZE";
    }

    @Override
    public BigDecimal freteCobrado(BigDecimal frete) {
        return frete;
    }

    @Override
    public BigDecimal credito(BigDecimal subtotalProdutos) {
        return BigDecimal.ZERO;
    }

    @Override
    public boolean brinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
