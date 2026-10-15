package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * O seguro é a mesma conta em toda região: só a alíquota muda. Por isso aqui
 * não há comportamento por caso, apenas o percentual de cada região.
 */
public enum Regiao {

    SUDESTE("1.0"),
    SUL("1.0"),
    CENTRO_OESTE("1.5"),
    NORTE("2.5"),
    NORDESTE("2.0");

    private final BigDecimal aliquotaSeguro;

    Regiao(String aliquotaSeguro) {
        this.aliquotaSeguro = new BigDecimal(aliquotaSeguro);
    }

    public static Optional<Regiao> porNome(String nome) {
        if (nome == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(valueOf(nome));
        } catch (IllegalArgumentException naoExiste) {
            return Optional.empty();
        }
    }

    public BigDecimal seguro(BigDecimal subtotalProdutos) {
        return Dinheiro.percentual(subtotalProdutos, aliquotaSeguro);
    }
}
