package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Optional;

/** A conta do imposto é a mesma em todas as regiões; só a alíquota muda. */
public enum Regiao {

    SUDESTE("12"),
    SUL("11"),
    CENTRO_OESTE("9"),
    NORTE("7"),
    NORDESTE("7");

    private final String aliquota;

    Regiao(String aliquota) {
        this.aliquota = aliquota;
    }

    public static Optional<Regiao> porNome(String nome) {
        return Arrays.stream(values()).filter(regiao -> regiao.name().equals(nome)).findFirst();
    }

    public BigDecimal imposto(BigDecimal produtosComDesconto) {
        return Dinheiro.percentual(produtosComDesconto, aliquota);
    }
}
