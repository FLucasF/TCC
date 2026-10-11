package br.com.loja.checkout.clube;

import br.com.loja.checkout.dominio.Carrinho;
import br.com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
class Ouro implements NivelClube {

    private static final BigDecimal PRODUTOS_PARA_BRINDE = new BigDecimal("500.00");

    public String codigo() {
        return "OURO";
    }

    public BigDecimal frete(BigDecimal freteModalidade) {
        return Dinheiro.ZERO;
    }

    public BigDecimal credito(Carrinho carrinho) {
        return Dinheiro.percentual(carrinho.subtotal(), "5");
    }

    public boolean brinde(Carrinho carrinho) {
        return carrinho.subtotal().compareTo(PRODUTOS_PARA_BRINDE) > 0;
    }
}
