package com.loja.checkout.dominio.entrega;

import com.loja.checkout.dominio.Dinheiro;

import java.math.BigDecimal;

public class RetiradaLoja implements Entrega {
    @Override public String codigo() { return "RETIRADA_LOJA"; }
    @Override public BigDecimal frete(BigDecimal pesoKg) { return Dinheiro.ZERO; }
    @Override public int prazoDias() { return 1; }
    @Override public boolean disponivelPara(BigDecimal pesoKg) { return true; }
}
