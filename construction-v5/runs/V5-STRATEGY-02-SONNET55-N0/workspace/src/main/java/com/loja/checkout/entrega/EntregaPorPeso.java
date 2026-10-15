package com.loja.checkout.entrega;

import com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;

abstract class EntregaPorPeso implements OpcaoEntrega {
    private final String codigo;
    private final BigDecimal base;
    private final BigDecimal porKg;
    private final int prazo;

    EntregaPorPeso(String codigo, String base, String porKg, int prazo) {
        this.codigo = codigo;
        this.base = new BigDecimal(base);
        this.porKg = new BigDecimal(porKg);
        this.prazo = prazo;
    }

    @Override
    public String codigo() {
        return codigo;
    }

    @Override
    public int prazoDias() {
        return prazo;
    }

    @Override
    public BigDecimal frete(BigDecimal pesoKg) {
        return Dinheiro.arredondar(base.add(porKg.multiply(pesoKg)));
    }
}
