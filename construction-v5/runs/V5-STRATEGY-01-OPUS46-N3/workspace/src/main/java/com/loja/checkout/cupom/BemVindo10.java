package com.loja.checkout.cupom;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class BemVindo10 implements Cupom {

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
    public BigDecimal calcularDesconto(ContextoCupom ctx) {
        return ctx.subtotal().multiply(PERCENTUAL).setScale(2, RoundingMode.HALF_EVEN);
    }
}
