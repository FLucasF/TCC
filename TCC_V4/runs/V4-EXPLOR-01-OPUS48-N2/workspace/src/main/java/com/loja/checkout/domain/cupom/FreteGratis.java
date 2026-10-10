package com.loja.checkout.domain.cupom;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** O cliente não paga o frete: o desconto fica igual ao valor do frete. */
@Component
public class FreteGratis implements Cupom {

    @Override
    public String codigo() {
        return "FRETEGRATIS";
    }

    @Override
    public BigDecimal desconto(CupomContexto ctx) {
        return ctx.frete();
    }
}
