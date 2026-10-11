package com.loja.checkout.cupom;

import com.loja.checkout.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
class BemVindo10 implements Cupom {

    @Override
    public String codigo() {
        return "BEMVINDO10";
    }

    @Override
    public BigDecimal desconto(ContextoCupom contexto) {
        return Dinheiro.percentual(contexto.subtotalProdutos(), "10");
    }
}
