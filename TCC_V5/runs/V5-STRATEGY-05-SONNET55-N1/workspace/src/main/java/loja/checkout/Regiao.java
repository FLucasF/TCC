package loja.checkout;

import java.math.BigDecimal;
import java.util.Optional;

/** Só a porcentagem do seguro muda de uma região para outra. */
public enum Regiao {
    SUDESTE("0.01"),
    SUL("0.01"),
    CENTRO_OESTE("0.015"),
    NORTE("0.025"),
    NORDESTE("0.02");

    private final String taxaSeguro;

    Regiao(String taxaSeguro) {
        this.taxaSeguro = taxaSeguro;
    }

    public BigDecimal seguro(BigDecimal subtotal) {
        return Dinheiro.percentual(subtotal, taxaSeguro);
    }

    public static Optional<Regiao> de(String codigo) {
        try {
            return Optional.of(valueOf(codigo));
        } catch (IllegalArgumentException | NullPointerException e) {
            return Optional.empty();
        }
    }
}
