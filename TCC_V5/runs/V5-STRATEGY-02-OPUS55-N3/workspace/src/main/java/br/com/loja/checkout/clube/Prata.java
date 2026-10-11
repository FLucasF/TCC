package br.com.loja.checkout.clube;

import br.com.loja.checkout.dominio.Carrinho;
import br.com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
class Prata implements NivelClube {

    public String codigo() {
        return "PRATA";
    }

    public BigDecimal frete(BigDecimal freteModalidade) {
        return freteModalidade;
    }

    public BigDecimal credito(Carrinho carrinho) {
        return Dinheiro.percentual(carrinho.subtotal(), "2");
    }

    public boolean brinde(Carrinho carrinho) {
        return false;
    }
}
