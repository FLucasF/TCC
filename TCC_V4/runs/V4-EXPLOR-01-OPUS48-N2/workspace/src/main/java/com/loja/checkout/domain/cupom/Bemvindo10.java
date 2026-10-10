package com.loja.checkout.domain.cupom;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** 10% de desconto no valor dos produtos. */
@Component
public class Bemvindo10 implements Cupom {

    private static final BigDecimal PERCENTUAL = new BigDecimal("0.10");

    @Override
    public String codigo() {
        return "BEMVINDO10";
    }

    @Override
    public BigDecimal desconto(CupomContexto ctx) {
        return ctx.subtotalProdutos().multiply(PERCENTUAL);
    }
}
