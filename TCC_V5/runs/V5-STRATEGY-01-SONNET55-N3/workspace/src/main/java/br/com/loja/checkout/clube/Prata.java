package br.com.loja.checkout.clube;

import br.com.loja.checkout.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class Prata implements NivelClube {

    public String codigo() {
        return "PRATA";
    }

    public Vantagens vantagens(BigDecimal subtotalProdutos) {
        return new Vantagens(Dinheiro.percentual(subtotalProdutos, "0.02"), false, false);
    }
}
