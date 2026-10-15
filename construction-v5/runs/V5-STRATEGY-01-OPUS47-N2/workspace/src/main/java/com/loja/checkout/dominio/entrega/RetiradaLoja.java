package com.loja.checkout.dominio.entrega;

import com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;

public final class RetiradaLoja implements Modalidade {
    public boolean atende(BigDecimal pesoKg) { return true; }
    public BigDecimal custo(BigDecimal pesoKg) { return Dinheiro.ZERO; }
    public int prazoDias() { return 1; }
}
