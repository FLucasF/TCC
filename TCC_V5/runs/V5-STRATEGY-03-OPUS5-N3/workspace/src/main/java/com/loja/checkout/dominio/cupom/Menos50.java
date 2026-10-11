package com.loja.checkout.dominio.cupom;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** R$ 50,00 nos produtos, a partir de R$ 300,00 em produtos. */
@Component
public class Menos50 implements Cupom {

    private static final BigDecimal VALOR = new BigDecimal("50.00");
    private static final BigDecimal MINIMO_PRODUTOS = new BigDecimal("300.00");

    @Override
    public String codigo() {
        return "MENOS50";
    }

    @Override
    public BigDecimal valorAbatido(ContextoCupom contexto) {
        return VALOR;
    }

    @Override
    public boolean aplicavel(ContextoCupom contexto) {
        return contexto.subtotal().compareTo(MINIMO_PRODUTOS) >= 0;
    }
}
