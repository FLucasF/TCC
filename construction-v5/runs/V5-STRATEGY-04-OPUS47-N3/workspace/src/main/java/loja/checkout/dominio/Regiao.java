package loja.checkout.dominio;

import java.math.BigDecimal;

import static loja.checkout.dominio.Dinheiro.centavos;

public enum Regiao {
    SUDESTE(new BigDecimal("0.01")),
    SUL(new BigDecimal("0.01")),
    CENTRO_OESTE(new BigDecimal("0.015")),
    NORTE(new BigDecimal("0.025")),
    NORDESTE(new BigDecimal("0.02"));

    private final BigDecimal percentual;

    Regiao(BigDecimal percentual) {
        this.percentual = percentual;
    }

    public BigDecimal seguro(BigDecimal subtotalProdutos) {
        return centavos(subtotalProdutos.multiply(percentual));
    }
}
