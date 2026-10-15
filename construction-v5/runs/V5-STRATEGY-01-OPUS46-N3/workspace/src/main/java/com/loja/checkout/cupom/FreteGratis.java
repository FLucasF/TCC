package com.loja.checkout.cupom;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class FreteGratis implements Cupom {

    @Override
    public String codigo() {
        return "FRETEGRATIS";
    }

    @Override
    public boolean aplicavel(ContextoCupom ctx) {
        return true;
    }

    @Override
    public BigDecimal calcularDesconto(ContextoCupom ctx) {
        return ctx.frete();
    }
}
