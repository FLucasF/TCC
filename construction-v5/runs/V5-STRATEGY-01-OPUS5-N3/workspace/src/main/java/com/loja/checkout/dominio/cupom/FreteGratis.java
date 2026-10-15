package com.loja.checkout.dominio.cupom;

import com.loja.checkout.dominio.Pedido;

import java.math.BigDecimal;

/** O frete aparece normalmente no resumo e o desconto fica igual a ele. */
public final class FreteGratis implements Cupom {

    @Override
    public String codigo() {
        return "FRETEGRATIS";
    }

    @Override
    public BigDecimal desconto(Pedido pedido, BigDecimal frete) {
        return frete;
    }
}
