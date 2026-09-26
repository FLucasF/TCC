package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.Optional;

/** So a aliquota muda de regiao para regiao; a conta do imposto e a mesma. */
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
    public BigDecimal imposto(BigDecimal produtosComDesconto) {
        return Dinheiro.percentual(Dinheiro.naoNegativo(produtosComDesconto), aliquota);
    }

    public static Optional<Regiao> porCodigo(String codigo) {
        return Codigos.buscar(Regiao.class, codigo);
    }
}
