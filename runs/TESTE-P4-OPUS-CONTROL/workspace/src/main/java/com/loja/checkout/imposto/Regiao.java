package com.loja.checkout.imposto;

import com.loja.checkout.dominio.Dinheiro;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Regiao do cliente. A conta do imposto e a mesma em todas, so a aliquota muda:
 * a porcentagem sobre os produtos ja com o desconto do cupom.
 */
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

    public BigDecimal aliquota() {
        return aliquota;
    }

    public BigDecimal imposto(BigDecimal produtosComDesconto) {
        return Dinheiro.percentual(produtosComDesconto, aliquota);
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
