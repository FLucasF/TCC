package com.loja.checkout.seguro;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Região do cliente e a taxa que a seguradora cobra nela. A conta é a mesma em todas
 * as regiões (percentual sobre os produtos), só a porcentagem muda.
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

    public BigDecimal taxaSeguro() {
        return taxaSeguro;
    }

    public static Optional<Regiao> buscar(String codigo) {
        if (codigo == null) {
            return Optional.empty();
        }
        for (Regiao regiao : values()) {
            if (regiao.name().equals(codigo)) {
                return Optional.of(regiao);
            }
        }
        return Optional.empty();
    }
}
