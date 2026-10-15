package br.com.loja.checkout.clube;

import br.com.loja.checkout.Carrinho;
import br.com.loja.checkout.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
class Ouro implements NivelClube {

    private static final BigDecimal PRODUTOS_PARA_BRINDE = new BigDecimal("500.00");

    @Override
    public String codigo() {
        return "OURO";
    }

    @Override
    public BigDecimal creditoProximaCompra(Carrinho carrinho) {
        return Dinheiro.percentual(carrinho.subtotal(), "5");
    }

    @Override
    public boolean isentaFrete() {
        return true;
    }

    @Override
    public boolean brinde(Carrinho carrinho) {
        return carrinho.subtotal().compareTo(PRODUTOS_PARA_BRINDE) > 0;
    }
}
