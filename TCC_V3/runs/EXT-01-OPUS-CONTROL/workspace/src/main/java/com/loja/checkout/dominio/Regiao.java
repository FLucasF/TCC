package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.Optional;

/** Regioes atendidas. So a aliquota muda, o calculo do imposto e o mesmo em todas. */
public enum Regiao {

    SUDESTE("0.12"),
    SUL("0.11"),
    CENTRO_OESTE("0.09"),
    NORTE("0.07"),
    NORDESTE("0.07");

    private final BigDecimal aliquota;

    Regiao(String aliquota) {
        this.aliquota = new BigDecimal(aliquota);
    }

    /** Imposto sobre os produtos ja com o desconto do cupom. */
    public BigDecimal imposto(BigDecimal baseTributavel) {
        return Moeda.percentual(baseTributavel, aliquota);
    }

    public static Optional<Regiao> porCodigo(String codigo) {
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
