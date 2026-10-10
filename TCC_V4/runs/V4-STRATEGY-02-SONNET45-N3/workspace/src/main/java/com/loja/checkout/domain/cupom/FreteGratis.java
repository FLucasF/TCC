package com.loja.checkout.domain.cupom;

import java.math.BigDecimal;

public class FreteGratis implements Cupom {

    @Override
    public BigDecimal calcularDesconto(ContextoCupom contexto) {
        return contexto.frete();
    }

    @Override
    public boolean aplicavel(ContextoCupom contexto) {
        return true;
    }
}
