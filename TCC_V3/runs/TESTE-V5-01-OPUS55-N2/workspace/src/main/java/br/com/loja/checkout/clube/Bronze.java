package br.com.loja.checkout.clube;

import br.com.loja.checkout.Carrinho;
import br.com.loja.checkout.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
class Bronze implements NivelClube {

    @Override
    public String codigo() {
        return "BRONZE";
    }

    @Override
    public BigDecimal creditoProximaCompra(Carrinho carrinho) {
        return Dinheiro.ZERO;
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
