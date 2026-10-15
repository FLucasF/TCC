package br.com.loja.checkout.clube;

import br.com.loja.checkout.Carrinho;
import br.com.loja.checkout.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
class Prata implements NivelClube {

    @Override
    public String codigo() {
        return "PRATA";
    }

    @Override
    public BigDecimal creditoProximaCompra(Carrinho carrinho) {
        return Dinheiro.percentual(carrinho.subtotal(), "2");
    }

    @Override
    public boolean isentaFrete() {
        return false;
    }

    @Override
    public boolean brinde(Carrinho carrinho) {
        return false;
    }
}
