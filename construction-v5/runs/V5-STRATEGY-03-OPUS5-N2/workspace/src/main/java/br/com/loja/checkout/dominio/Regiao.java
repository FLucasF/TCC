package br.com.loja.checkout.dominio;

import java.math.BigDecimal;

/**
 * Regiao do cliente. O seguro contra extravio e roubo e sempre a mesma conta,
 * a porcentagem sobre o valor dos produtos; o que muda e a porcentagem.
 */
public enum Regiao implements Codificavel {

    SUDESTE("0.010"),
    SUL("0.010"),
    CENTRO_OESTE("0.015"),
    NORTE("0.025"),
    NORDESTE("0.020");

    private final BigDecimal aliquotaSeguro;

    Regiao(String aliquotaSeguro) {
        this.aliquotaSeguro = new BigDecimal(aliquotaSeguro);
    }

    @Override
    public String codigo() {
        return name();
    }

    public BigDecimal seguro(BigDecimal subtotalProdutos) {
        return Dinheiro.percentual(subtotalProdutos, aliquotaSeguro);
    }
}
