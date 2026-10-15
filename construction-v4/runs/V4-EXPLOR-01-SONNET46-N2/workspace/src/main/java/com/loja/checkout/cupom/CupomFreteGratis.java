package com.loja.checkout.cupom;

import java.math.BigDecimal;

public class CupomFreteGratis implements Cupom {

    @Override
    public boolean isAplicavel(CupomContexto ctx) {
        return true;
    }

    @Override
    public BigDecimal calcularDesconto(CupomContexto ctx) {
        return ctx.frete();
    }
}
