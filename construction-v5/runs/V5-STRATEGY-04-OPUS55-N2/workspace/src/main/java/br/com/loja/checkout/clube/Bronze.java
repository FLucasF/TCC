package br.com.loja.checkout.clube;

import br.com.loja.checkout.dominio.Carrinho;
import br.com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
class Bronze implements NivelClube {

    @Override
    public String codigo() {
        return "BRONZE";
    }

    @Override
    public BigDecimal credito(Carrinho carrinho) {
        return Dinheiro.ZERO;
    }

    @Override
    public BigDecimal frete(BigDecimal freteEntrega) {
        return freteEntrega;
    }

    @Override
    public boolean brinde(Carrinho carrinho) {
        return false;
    }
}
