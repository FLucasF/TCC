package com.loja.checkout.cupom;

import com.loja.checkout.comum.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
class BemVindo10 implements Cupom {

    private static final BigDecimal PERCENTUAL = BigDecimal.TEN;

    @Override
    public String codigo() {
        return "BEMVINDO10";
    }

    @Override
    public boolean aplicavel(ContextoCupom contexto) {
        return true;
    }

    @Override
    public BigDecimal desconto(ContextoCupom contexto) {
        return Dinheiro.percentual(contexto.carrinho().subtotal(), PERCENTUAL);
    }
}
