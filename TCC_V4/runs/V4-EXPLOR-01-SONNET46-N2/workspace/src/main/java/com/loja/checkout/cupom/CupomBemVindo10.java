package com.loja.checkout.cupom;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class CupomBemVindo10 implements Cupom {

    @Override
    public boolean isAplicavel(CupomContexto ctx) {
        return true;
    }

    @Override
    public BigDecimal calcularDesconto(CupomContexto ctx) {
        return ctx.subtotalProdutos()
                .multiply(new BigDecimal("0.10"))
                .setScale(2, RoundingMode.HALF_EVEN);
    }
}
