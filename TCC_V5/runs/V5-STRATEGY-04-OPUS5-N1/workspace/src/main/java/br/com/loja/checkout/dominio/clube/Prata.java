package br.com.loja.checkout.dominio.clube;

import java.math.BigDecimal;

import br.com.loja.checkout.dominio.Dinheiro;

final class Prata implements NivelClube {

    private static final BigDecimal PERCENTUAL_CREDITO = Dinheiro.de("2");

    @Override
    public String codigo() {
        return "PRATA";
    }

    @Override
    public BigDecimal creditoProximaCompra(BigDecimal subtotalProdutos) {
        return Dinheiro.percentual(PERCENTUAL_CREDITO, subtotalProdutos);
    }

    @Override
    public boolean isentaFrete() {
        return false;
    }

    @Override
    public boolean ganhaBrinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
