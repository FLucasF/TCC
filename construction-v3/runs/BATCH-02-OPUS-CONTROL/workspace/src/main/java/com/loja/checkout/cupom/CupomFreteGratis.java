package com.loja.checkout.cupom;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** O cliente nao paga o frete: o desconto fica igual ao valor do frete. */
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
