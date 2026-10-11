package br.com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.List;

/**
 * A conta do seguro é a mesma em toda região: a porcentagem sobre o valor dos
 * produtos. Só a porcentagem muda de região para região.
 */
public enum Regiao {

    SUDESTE("1"),
    SUL("1"),
    CENTRO_OESTE("1.5"),
    NORTE("2.5"),
    NORDESTE("2");

    public static final Catalogo<Regiao> CATALOGO =
            Catalogo.de(MotivoRecusa.REGIAO_INVALIDA, Regiao::name, List.of(values()));

    private final BigDecimal percentualSeguro;

    Regiao(String percentualSeguro) {
        this.percentualSeguro = Dinheiro.de(percentualSeguro);
    }

    public BigDecimal seguro(BigDecimal subtotalProdutos) {
        return Dinheiro.percentual(percentualSeguro, subtotalProdutos);
    }
}
