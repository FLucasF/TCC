package com.loja.checkout.domain.cupom;

import java.math.BigDecimal;

public class CupomFreteGratis implements Cupom {

    @Override
    public BigDecimal calcularDesconto(ContextoCupom contexto) {
        return contexto.frete();
    }

    @Override
    public boolean eAplicavel(ContextoCupom contexto) {
        return true;
    }
}
