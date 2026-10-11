package com.loja.checkout.domain.entrega;

import java.math.BigDecimal;

class RetiradaLoja implements ModalidadeEntrega {

    @Override
    public String codigo() { return "RETIRADA_LOJA"; }

    @Override
    public boolean disponivel(BigDecimal pesoKg) { return true; }

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoKg) { return new BigDecimal("0.00"); }

    @Override
    public int prazoDias() { return 1; }
}
