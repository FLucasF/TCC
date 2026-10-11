package br.com.loja.checkout.clube;

import br.com.loja.checkout.Dinheiro;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
class Prata implements NivelClube {

    @Override
    public String codigo() {
        return "PRATA";
    }

    @Override
    public BigDecimal credito(BigDecimal subtotalProdutos) {
        return Dinheiro.percentual(subtotalProdutos, "0.02");
    }
}
