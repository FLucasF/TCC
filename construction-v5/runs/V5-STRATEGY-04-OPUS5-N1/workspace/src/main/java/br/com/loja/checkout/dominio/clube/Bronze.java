package br.com.loja.checkout.dominio.clube;

import java.math.BigDecimal;

import br.com.loja.checkout.dominio.Dinheiro;

final class Bronze implements NivelClube {

    @Override
    public String codigo() {
        return "BRONZE";
    }

    @Override
    public BigDecimal creditoProximaCompra(BigDecimal subtotalProdutos) {
        return Dinheiro.ZERO;
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
