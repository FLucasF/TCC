package br.com.loja.checkout.clube;

import br.com.loja.checkout.resumo.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
class Prata implements NivelClube {

    @Override
    public String codigo() {
        return "PRATA";
    }

    @Override
    public Vantagens vantagens(BigDecimal subtotalProdutos) {
        return new Vantagens(Dinheiro.percentual(subtotalProdutos, "2"), false, false);
    }
}
