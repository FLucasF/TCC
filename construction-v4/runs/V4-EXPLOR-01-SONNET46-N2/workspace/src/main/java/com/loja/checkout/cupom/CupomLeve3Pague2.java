package com.loja.checkout.cupom;

import java.math.BigDecimal;

public class CupomLeve3Pague2 implements Cupom {

    @Override
    public boolean isAplicavel(CupomContexto ctx) {
        return true;
    }

    @Override
    public BigDecimal calcularDesconto(CupomContexto ctx) {
        return ctx.itens().stream()
                .map(item -> {
                    int gratis = item.quantidade() / 3;
                    return item.precoUnitario().multiply(BigDecimal.valueOf(gratis));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
