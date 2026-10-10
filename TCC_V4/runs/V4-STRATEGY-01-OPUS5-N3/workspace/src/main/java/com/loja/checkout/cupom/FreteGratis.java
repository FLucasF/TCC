package com.loja.checkout.cupom;


import java.math.BigDecimal;

/**
 * O cliente nao paga o frete: o frete aparece normalmente no resumo e o
 * desconto do cupom fica igual ao valor dele.
 */
public final class FreteGratis implements Cupom {

    @Override
    public String codigo() {
        return "FRETEGRATIS";
    }

    @Override
    public boolean aplicavel(ContextoCupom contexto) {
        return true;
    }

    @Override
    public BigDecimal desconto(ContextoCupom contexto) {
        return contexto.frete();
    }
}
