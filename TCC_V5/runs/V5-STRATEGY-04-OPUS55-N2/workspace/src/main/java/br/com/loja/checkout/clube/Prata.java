package br.com.loja.checkout.clube;

import br.com.loja.checkout.dominio.Carrinho;
import br.com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
class Prata implements NivelClube {

    private static final BigDecimal PERCENTUAL_CREDITO = new BigDecimal("2");

    @Override
    public String codigo() {
        return "PRATA";
    }

    @Override
    public BigDecimal credito(Carrinho carrinho) {
        return Dinheiro.percentual(carrinho.subtotal(), PERCENTUAL_CREDITO);
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
