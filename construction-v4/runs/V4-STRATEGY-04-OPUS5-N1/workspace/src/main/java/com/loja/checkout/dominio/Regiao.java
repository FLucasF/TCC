package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.List;

/**
 * Regiao do cliente. O seguro do envio e sempre a mesma conta - a porcentagem
 * sobre o valor dos produtos - so a porcentagem muda de regiao para regiao.
 */
public enum Regiao {

    SUDESTE("0.010"),
    SUL("0.010"),
    CENTRO_OESTE("0.015"),
    NORTE("0.025"),
    NORDESTE("0.020");

    private final BigDecimal taxaSeguro;

    Regiao(String taxaSeguro) {
        this.taxaSeguro = new BigDecimal(taxaSeguro);
    }

    /** Seguro contra extravio e roubo, sobre o valor dos produtos. */
    public BigDecimal seguro(BigDecimal subtotalProdutos) {
        return Centavos.percentual(subtotalProdutos, taxaSeguro);
    }

    public static final Catalogo<Regiao> CATALOGO =
            Catalogo.de(CodigoErro.REGIAO_INVALIDA, Regiao::name, List.of(values()));
}
