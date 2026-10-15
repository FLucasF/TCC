package com.loja.checkout.resumo.entrega;

import com.loja.checkout.resumo.Dinheiro;
import java.math.BigDecimal;

/** R$ 12,00 + R$ 2,00 por kg, em 7 dias. */
public class Economica implements ModalidadeEntrega {

    private static final BigDecimal BASE = new BigDecimal("12.00");
    private static final BigDecimal POR_KG = new BigDecimal("2.00");

    @Override
    public BigDecimal frete(BigDecimal pesoKg) {
        return Dinheiro.centavos(BASE.add(POR_KG.multiply(pesoKg)));
    }

    @Override
    public int prazoDias() {
        return 7;
    }
}
