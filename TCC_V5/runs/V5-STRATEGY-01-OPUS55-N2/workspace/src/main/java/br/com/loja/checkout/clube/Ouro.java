package br.com.loja.checkout.clube;

import br.com.loja.checkout.resumo.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
class Ouro implements NivelClube {

    private static final BigDecimal MINIMO_BRINDE = new BigDecimal("500.00");

    @Override
    public String codigo() {
        return "OURO";
    }

    @Override
    public Vantagens vantagens(BigDecimal subtotalProdutos) {
        return new Vantagens(
                Dinheiro.percentual(subtotalProdutos, "5"),
                true,
                subtotalProdutos.compareTo(MINIMO_BRINDE) > 0);
    }
}
