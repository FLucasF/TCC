package com.loja.checkout.dominio.cupom;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * O cliente não paga o frete: o frete aparece normalmente no resumo e o desconto
 * do cupom fica igual ao valor do frete (efetivo).
 */
@Component
public class FreteGratis implements Cupom {

    @Override
    public String codigo() {
        return "FRETEGRATIS";
    }

    @Override
    public BigDecimal desconto(ContextoCupom ctx) {
        return ctx.frete();
    }
}
