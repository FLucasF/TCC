package br.com.loja.checkout.clube;

import br.com.loja.checkout.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class Prata implements NivelClube {
    public String codigo() { return "PRATA"; }

    public BigDecimal credito(BigDecimal subtotalProdutos) {
        return Dinheiro.percentual(subtotalProdutos, "0.02");
    }
}
