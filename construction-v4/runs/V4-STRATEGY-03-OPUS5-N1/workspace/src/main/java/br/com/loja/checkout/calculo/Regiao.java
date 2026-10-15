package br.com.loja.checkout.calculo;

import br.com.loja.checkout.catalogo.Catalogo;
import br.com.loja.checkout.catalogo.Codificado;
import java.math.BigDecimal;
import java.util.List;

/**
 * Regiao do cliente. A conta do seguro e a mesma em todas as regioes: so a
 * taxa da seguradora muda.
 */
public enum Regiao implements Codificado {

    SUDESTE("0.010"),
    SUL("0.010"),
    CENTRO_OESTE("0.015"),
    NORTE("0.025"),
    NORDESTE("0.020");

    public static final Catalogo<Regiao> CATALOGO = new Catalogo<>(List.of(values()));

    private final BigDecimal taxaSeguro;

    Regiao(String taxaSeguro) {
        this.taxaSeguro = new BigDecimal(taxaSeguro);
    }

    @Override
    public String codigo() {
        return name();
    }

    public BigDecimal seguro(BigDecimal subtotalProdutos) {
        return Dinheiro.percentual(subtotalProdutos, taxaSeguro);
    }
}
