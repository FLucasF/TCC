package com.loja.checkout.dominio.entrega;

import com.loja.checkout.dominio.Dinheiro;

import java.math.BigDecimal;

/** O cliente busca na loja: nao paga frete e fica pronto no dia seguinte. */
public final class RetiradaNaLoja implements Entrega {

    private static final int PRAZO_DIAS = 1;

    @Override
    public Frete calcular(BigDecimal pesoKg) {
        return new Frete(Dinheiro.ZERO, PRAZO_DIAS);
    }
}
