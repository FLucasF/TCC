package com.loja.checkout.dominio.cupom;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/**
 * O cliente não paga o frete: o frete aparece normalmente no resumo e o
 * desconto do cupom fica igual ao valor do frete.
 */
@Component
public class CupomFreteGratis implements Cupom {

    @Override
    public String codigo() {
        return "FRETEGRATIS";
    }

    @Override
    public BigDecimal desconto(ContextoCupom ctx) {
        return ctx.frete();
    }
}
