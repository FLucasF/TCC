package br.com.loja.checkout.seguro;

import br.com.loja.checkout.pedido.Dinheiro;
import br.com.loja.checkout.pedido.Opcao;
import java.math.BigDecimal;

/** Região do cliente; só muda a taxa do seguro, a conta é a mesma. */
public enum Regiao implements Opcao {
    SUDESTE("0.01"),
    SUL("0.01"),
    CENTRO_OESTE("0.015"),
    NORTE("0.025"),
    NORDESTE("0.02");

    private final BigDecimal taxaSeguro;

    Regiao(String taxaSeguro) {
        this.taxaSeguro = new BigDecimal(taxaSeguro);
    }

    @Override
    public String codigo() {
        return name();
    }

    public BigDecimal seguro(BigDecimal subtotalProdutos) {
        return Dinheiro.aplicarTaxa(subtotalProdutos, taxaSeguro);
    }
}
