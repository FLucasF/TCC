package br.com.loja.checkout.dominio.clube;

import java.math.BigDecimal;

import br.com.loja.checkout.dominio.Dinheiro;

final class Ouro implements NivelClube {

    private static final BigDecimal PERCENTUAL_CREDITO = Dinheiro.de("5");
    private static final BigDecimal SUBTOTAL_PARA_BRINDE = Dinheiro.de("500.00");

    @Override
    public String codigo() {
        return "OURO";
    }

    @Override
    public BigDecimal creditoProximaCompra(BigDecimal subtotalProdutos) {
        return Dinheiro.percentual(PERCENTUAL_CREDITO, subtotalProdutos);
    }

    @Override
    public boolean isentaFrete() {
        return true;
    }

    @Override
    public boolean ganhaBrinde(BigDecimal subtotalProdutos) {
        return subtotalProdutos.compareTo(SUBTOTAL_PARA_BRINDE) > 0;
    }
}
