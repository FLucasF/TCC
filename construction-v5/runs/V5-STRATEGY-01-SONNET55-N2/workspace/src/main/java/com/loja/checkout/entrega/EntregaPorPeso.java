package com.loja.checkout.entrega;

import com.loja.checkout.dominio.Carrinho;
import com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;

/** Frete com valor fixo mais um valor por kg; sem limite de peso. */
abstract class EntregaPorPeso implements Entrega {

    private final String codigo;
    private final BigDecimal base;
    private final BigDecimal porKg;
    private final int prazoDias;

    EntregaPorPeso(String codigo, String base, String porKg, int prazoDias) {
        this.codigo = codigo;
        this.base = new BigDecimal(base);
        this.porKg = new BigDecimal(porKg);
        this.prazoDias = prazoDias;
    }

    @Override
    public String codigo() {
        return codigo;
    }

    @Override
    public boolean disponivelPara(Carrinho carrinho) {
        return true;
    }

    @Override
    public BigDecimal frete(Carrinho carrinho) {
        return Dinheiro.arredondar(base.add(porKg.multiply(carrinho.pesoKg())));
    }

    @Override
    public int prazoDias() {
        return prazoDias;
    }
}
