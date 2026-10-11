package br.com.loja.checkout.clube;

import br.com.loja.checkout.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class Ouro implements NivelClube {

    private static final BigDecimal LIMITE_BRINDE = new BigDecimal("500.00");

    public String codigo() {
        return "OURO";
    }

    public Vantagens vantagens(BigDecimal subtotalProdutos) {
        return new Vantagens(
                Dinheiro.percentual(subtotalProdutos, "0.05"),
                true,
                subtotalProdutos.compareTo(LIMITE_BRINDE) > 0);
    }
}
