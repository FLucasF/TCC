package com.loja.checkout.dominio.entrega;

import com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;

public final class Expressa implements Modalidade {
    private static final BigDecimal BASE = new BigDecimal("25.00");
    private static final BigDecimal POR_KG = new BigDecimal("4.50");

    public boolean atende(BigDecimal pesoKg) { return true; }

    public BigDecimal custo(BigDecimal pesoKg) {
        return Dinheiro.arredondar(BASE.add(POR_KG.multiply(pesoKg)));
    }

    public int prazoDias() { return 2; }
}
