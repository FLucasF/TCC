package com.loja.checkout.domain.cupom;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** O cliente não paga o frete: o desconto do cupom é igual ao valor do frete. */
@Component
public class FreteGratis implements Cupom {

    @Override
    public String codigo() {
        return "FRETEGRATIS";
    }

    @Override
    public boolean aplicavel(CupomContexto ctx) {
        return true;
    }

    @Override
    public BigDecimal desconto(CupomContexto ctx) {
        return ctx.frete();
    }
}
