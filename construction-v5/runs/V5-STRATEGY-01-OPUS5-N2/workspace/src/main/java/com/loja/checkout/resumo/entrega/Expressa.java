package com.loja.checkout.resumo.entrega;

import com.loja.checkout.resumo.Dinheiro;
import java.math.BigDecimal;

/** R$ 25,00 + R$ 4,50 por kg, em 2 dias. */
public class Expressa implements ModalidadeEntrega {

    private static final BigDecimal BASE = new BigDecimal("25.00");
    private static final BigDecimal POR_KG = new BigDecimal("4.50");

    @Override
    public BigDecimal frete(BigDecimal pesoKg) {
        return Dinheiro.centavos(BASE.add(POR_KG.multiply(pesoKg)));
    }

    @Override
    public int prazoDias() {
        return 2;
    }
}
