package com.loja.checkout.cupom;

import com.loja.checkout.Dinheiro;
import com.loja.checkout.Pedido;
import java.math.BigDecimal;

/** Pedido sem cupom: não é um bean, o serviço usa quando o cupom não vem. */
public final class SemCupom implements Cupom {
    public static final SemCupom INSTANCIA = new SemCupom();

    private SemCupom() {
    }

    @Override
    public String codigo() {
        return "";
    }

    @Override
    public boolean aplicavel(Pedido pedido) {
        return true;
    }

    @Override
    public BigDecimal desconto(Pedido pedido, BigDecimal frete) {
        return Dinheiro.ZERO;
    }
}
