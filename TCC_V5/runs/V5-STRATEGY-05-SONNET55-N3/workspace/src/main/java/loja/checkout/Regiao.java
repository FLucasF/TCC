package loja.checkout;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Optional;

/** Só a porcentagem do seguro muda de região para região. */
public enum Regiao {
    SUDESTE("0.01"),
    SUL("0.01"),
    CENTRO_OESTE("0.015"),
    NORTE("0.025"),
    NORDESTE("0.02");

    private final BigDecimal percentualSeguro;

    Regiao(String percentualSeguro) {
        this.percentualSeguro = new BigDecimal(percentualSeguro);
    }

    public BigDecimal seguro(BigDecimal subtotalProdutos) {
        return Dinheiro.percentual(subtotalProdutos, percentualSeguro);
    }

    public static Optional<Regiao> de(String codigo) {
        return Arrays.stream(values()).filter(r -> r.name().equals(codigo)).findFirst();
    }
}
