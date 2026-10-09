package loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.Optional;

public enum Regiao {
    SUDESTE("0.01"),
    SUL("0.01"),
    CENTRO_OESTE("0.015"),
    NORTE("0.025"),
    NORDESTE("0.02");

    private final BigDecimal taxaSeguro;

    Regiao(String taxaSeguro) {
        this.taxaSeguro = new BigDecimal(taxaSeguro);
    }

    public BigDecimal seguro(Pedido pedido) {
        return Dinheiro.arredondar(pedido.subtotal().multiply(taxaSeguro));
    }

    public static Optional<Regiao> de(String codigo) {
        for (Regiao r : values()) {
            if (r.name().equals(codigo)) {
                return Optional.of(r);
            }
        }
        return Optional.empty();
    }
}
