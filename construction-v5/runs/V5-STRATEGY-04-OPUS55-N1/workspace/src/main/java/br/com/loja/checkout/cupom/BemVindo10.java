package br.com.loja.checkout.cupom;

import br.com.loja.checkout.Dinheiro;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
class BemVindo10 implements Cupom {

    private static final BigDecimal PORCENTAGEM = new BigDecimal("10");

    @Override
    public String codigo() {
        return "BEMVINDO10";
    }

    @Override
    public BigDecimal desconto(ContextoCupom contexto) {
        return Dinheiro.percentual(contexto.carrinho().subtotal(), PORCENTAGEM);
    }
}
