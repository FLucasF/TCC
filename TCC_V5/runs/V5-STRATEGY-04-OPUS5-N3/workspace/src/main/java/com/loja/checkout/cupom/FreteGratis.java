package com.loja.checkout.cupom;

import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.Pedido;

import java.math.BigDecimal;

/** O cliente não paga o frete: o desconto fica igual ao valor do frete. */
class FreteGratis implements Cupom {

    @Override
    public String codigo() {
        return "FRETEGRATIS";
    }

    @Override
    public BigDecimal desconto(Pedido pedido, BigDecimal frete) {
        return Dinheiro.centavos(frete);
    }
}
