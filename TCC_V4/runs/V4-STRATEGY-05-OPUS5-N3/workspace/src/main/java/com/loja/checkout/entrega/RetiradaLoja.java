package com.loja.checkout.entrega;

import com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;

/** O cliente busca na loja: gratis, pronto em 1 dia. */
public final class RetiradaLoja implements ModalidadeEntrega {

    @Override
    public String codigo() {
        return "RETIRADA_LOJA";
    }

    @Override
    public BigDecimal frete(BigDecimal pesoKg) {
        return Dinheiro.ZERO;
    }

    @Override
    public int prazoDias() {
        return 1;
    }
}
