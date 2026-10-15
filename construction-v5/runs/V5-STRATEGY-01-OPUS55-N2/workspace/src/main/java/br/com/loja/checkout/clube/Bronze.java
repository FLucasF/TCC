package br.com.loja.checkout.clube;

import br.com.loja.checkout.resumo.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
class Bronze implements NivelClube {

    @Override
    public String codigo() {
        return "BRONZE";
    }

    @Override
    public Vantagens vantagens(BigDecimal subtotalProdutos) {
        return new Vantagens(Dinheiro.ZERO, false, false);
    }
}
