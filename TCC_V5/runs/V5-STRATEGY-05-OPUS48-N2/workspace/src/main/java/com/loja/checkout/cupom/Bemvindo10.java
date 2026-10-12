package com.loja.checkout.cupom;

import com.loja.checkout.comum.Dinheiro;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class Bemvindo10 implements Cupom {

    private static final BigDecimal PERCENTUAL = new BigDecimal("0.10");

    @Override
    public String codigo() {
        return "BEMVINDO10";
    }

    @Override
    public boolean aplicavel(ContextoCupom ctx) {
        return true;
    }

    @Override
    public BigDecimal desconto(ContextoCupom ctx) {
        return Dinheiro.centavos(ctx.subtotalProdutos().multiply(PERCENTUAL));
    }
}
