package com.loja.checkout.cupom;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** O cliente não paga o frete: o frete aparece no resumo e o desconto fica igual a ele. */
@Component
public class CupomFreteGratis implements Cupom {

    @Override
    public String codigo() {
        return "FRETEGRATIS";
    }

    @Override
    public BigDecimal desconto(ContextoCupom contexto) {
        return contexto.frete();
    }
}
