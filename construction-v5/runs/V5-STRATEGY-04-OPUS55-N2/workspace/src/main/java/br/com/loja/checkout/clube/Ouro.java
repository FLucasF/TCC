package br.com.loja.checkout.clube;

import br.com.loja.checkout.dominio.Carrinho;
import br.com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
class Ouro implements NivelClube {

    private static final BigDecimal PERCENTUAL_CREDITO = new BigDecimal("5");
    private static final BigDecimal PRODUTOS_PARA_BRINDE = new BigDecimal("500.00");

    @Override
    public String codigo() {
        return "OURO";
    }

    @Override
    public BigDecimal credito(Carrinho carrinho) {
        return Dinheiro.percentual(carrinho.subtotal(), PERCENTUAL_CREDITO);
    }

    /** OURO não paga frete nunca. */
    @Override
    public BigDecimal frete(BigDecimal freteEntrega) {
        return Dinheiro.ZERO;
    }

    @Override
    public boolean brinde(Carrinho carrinho) {
        return carrinho.subtotal().compareTo(PRODUTOS_PARA_BRINDE) > 0;
    }
}
