package br.com.loja.checkout.clube;

import br.com.loja.checkout.dominio.Carrinho;
import br.com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
class Bronze implements NivelClube {

    public String codigo() {
        return "BRONZE";
    }

    public BigDecimal frete(BigDecimal freteModalidade) {
        return freteModalidade;
    }

    public BigDecimal credito(Carrinho carrinho) {
        return Dinheiro.ZERO;
    }

    public boolean brinde(Carrinho carrinho) {
        return false;
    }
}
