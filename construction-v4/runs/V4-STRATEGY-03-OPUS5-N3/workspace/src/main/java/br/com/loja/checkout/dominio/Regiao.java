package br.com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Optional;

/**
 * Regiao do cliente. Entre as regioes muda apenas a taxa do seguro de envio;
 * a conta e a mesma em todas, por isso aqui basta a taxa.
 */
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

    public static Optional<Regiao> porCodigo(String codigo) {
        return Arrays.stream(values()).filter(regiao -> regiao.name().equals(codigo)).findFirst();
    }

    /** Seguro contra extravio e roubo: taxa da regiao sobre o valor dos produtos. */
    public Dinheiro seguro(Dinheiro subtotalProdutos) {
        return subtotalProdutos.vezes(taxaSeguro);
    }
}
