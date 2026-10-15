package com.loja.checkout.resumo.entrega;

import com.loja.checkout.resumo.Dinheiro;
import java.math.BigDecimal;

/** Grátis, pronta para retirada em 1 dia. */
public class RetiradaLoja implements ModalidadeEntrega {

    @Override
    public BigDecimal frete(BigDecimal pesoKg) {
        return Dinheiro.ZERO;
    }

    @Override
    public int prazoDias() {
        return 1;
    }
}
