package com.loja.checkout.dominio.clube;

import com.loja.checkout.dominio.Dinheiro;

import java.math.BigDecimal;

/** Nivel que devolve um percentual dos produtos em credito. */
public abstract class ClubePorPercentual implements PoliticaClube {

    private final BigDecimal percentualCredito;

    protected ClubePorPercentual(String percentualCredito) {
        this.percentualCredito = new BigDecimal(percentualCredito);
    }

    @Override
    public BigDecimal creditoProximaCompra(BigDecimal subtotalProdutos) {
        return Dinheiro.percentual(subtotalProdutos, percentualCredito);
    }

    @Override
    public boolean isentaFrete() {
        return false;
    }

    @Override
    public boolean temBrinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
