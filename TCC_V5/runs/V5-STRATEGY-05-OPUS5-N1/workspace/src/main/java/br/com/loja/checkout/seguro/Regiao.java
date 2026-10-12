package br.com.loja.checkout.seguro;

import br.com.loja.checkout.comum.Catalogo;
import br.com.loja.checkout.comum.Codificado;
import br.com.loja.checkout.comum.Dinheiro;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * A região do cliente. A conta do seguro é a mesma em todas: só a
 * porcentagem sobre o valor dos produtos muda.
 */
public enum Regiao implements Codificado {

    SUDESTE("0.01"),
    SUL("0.01"),
    CENTRO_OESTE("0.015"),
    NORTE("0.025"),
    NORDESTE("0.02");

    private static final Catalogo<Regiao> CATALOGO = new Catalogo<>(List.of(values()));

    private final BigDecimal taxaSeguro;

    Regiao(String taxaSeguro) {
        this.taxaSeguro = new BigDecimal(taxaSeguro);
    }

    public static Optional<Regiao> buscar(String codigo) {
        return CATALOGO.buscar(codigo);
    }

    @Override
    public String codigo() {
        return name();
    }

    public BigDecimal seguro(BigDecimal subtotalProdutos) {
        return Dinheiro.percentual(subtotalProdutos, taxaSeguro);
    }
}
