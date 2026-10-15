package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Região do cliente. A conta do seguro é a mesma em todas: só a alíquota muda,
 * então aqui basta guardar a alíquota.
 */
public enum Regiao {

    SUDESTE("0.01"),
    SUL("0.01"),
    CENTRO_OESTE("0.015"),
    NORTE("0.025"),
    NORDESTE("0.02");

    private final BigDecimal aliquotaSeguro;

    Regiao(String aliquotaSeguro) {
        this.aliquotaSeguro = new BigDecimal(aliquotaSeguro);
    }

    public BigDecimal seguro(BigDecimal subtotalProdutos) {
        return Dinheiro.percentual(subtotalProdutos, aliquotaSeguro);
    }

    public static Optional<Regiao> porCodigo(String codigo) {
        return Registro.busca(List.of(values()), Enum::name, codigo);
    }
}
