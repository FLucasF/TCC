package com.loja.checkout.cupom;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class Bemvindo10Cupom implements Cupom {

    private static final BigDecimal PERCENTUAL = new BigDecimal("0.10");

    @Override
    public String codigo() {
        return "BEMVINDO10";
    }

    @Override
    public boolean aplicavel(ContextoCupom contexto) {
        return true;
    }

    @Override
    public BigDecimal calcularDesconto(ContextoCupom contexto) {
        return contexto.subtotalProdutos().multiply(PERCENTUAL);
    }
}
