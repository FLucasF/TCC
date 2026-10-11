package com.loja.checkout.dominio.entrega;

import com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;

public final class Economica implements Modalidade {
    private static final BigDecimal BASE = new BigDecimal("12.00");
    private static final BigDecimal POR_KG = new BigDecimal("2.00");

    public boolean atende(BigDecimal pesoKg) { return true; }

    public BigDecimal custo(BigDecimal pesoKg) {
        return Dinheiro.arredondar(BASE.add(POR_KG.multiply(pesoKg)));
    }

    public int prazoDias() { return 7; }
}
