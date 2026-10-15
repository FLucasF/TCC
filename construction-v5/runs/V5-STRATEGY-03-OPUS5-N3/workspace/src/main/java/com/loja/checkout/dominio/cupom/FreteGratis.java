package com.loja.checkout.dominio.cupom;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** O cliente não paga o frete: o desconto fica igual ao valor do frete. */
@Component
public class FreteGratis implements Cupom {

    @Override
    public String codigo() {
        return "FRETEGRATIS";
    }

    @Override
    public BigDecimal valorAbatido(ContextoCupom contexto) {
        return contexto.frete();
    }
}
