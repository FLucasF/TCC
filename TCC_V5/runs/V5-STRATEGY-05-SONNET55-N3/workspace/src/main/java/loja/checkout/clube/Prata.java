package loja.checkout.clube;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import loja.checkout.Dinheiro;

@Component
public class Prata implements NivelClube {
    private static final BigDecimal CREDITO = new BigDecimal("0.02");

    @Override
    public String codigo() {
        return "PRATA";
    }

    @Override
    public BigDecimal credito(BigDecimal subtotalProdutos) {
        return Dinheiro.percentual(subtotalProdutos, CREDITO);
    }
}
