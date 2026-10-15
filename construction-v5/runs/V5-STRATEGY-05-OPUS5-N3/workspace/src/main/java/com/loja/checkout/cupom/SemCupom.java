package com.loja.checkout.cupom;

import com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;

/** Pedido sem cupom: desconto zero, para o cálculo não ter ramo especial. */
public class SemCupom implements Cupom {

    @Override
    public String codigo() {
        return "";
    }

    @Override
    public BigDecimal desconto(ContextoCupom contexto) {
        return Dinheiro.ZERO;
    }
}
