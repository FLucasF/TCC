package br.com.loja.checkout.clube;

import br.com.loja.checkout.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class Bronze implements NivelClube {

    public String codigo() {
        return "BRONZE";
    }

    public Vantagens vantagens(BigDecimal subtotalProdutos) {
        return new Vantagens(Dinheiro.ZERO, false, false);
    }
}
