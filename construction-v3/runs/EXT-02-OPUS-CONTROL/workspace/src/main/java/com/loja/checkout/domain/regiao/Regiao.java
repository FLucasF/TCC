package com.loja.checkout.domain.regiao;

import com.loja.checkout.domain.pedido.Dinheiro;
import java.math.BigDecimal;
import java.util.Optional;

/**
 * Regiao do cliente. Em todas a conta do imposto e a mesma (percentual sobre os
 * produtos ja com o desconto do cupom); so a aliquota muda.
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

    /** Imposto sobre os produtos ja descontados, arredondado para centavos. */
    public BigDecimal imposto(BigDecimal produtosComDesconto) {
        return Dinheiro.percentual(produtosComDesconto, aliquota);
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
