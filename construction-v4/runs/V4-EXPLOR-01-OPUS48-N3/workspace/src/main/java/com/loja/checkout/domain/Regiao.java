package com.loja.checkout.domain;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Região do cliente. A conta do seguro é a mesma em todas (percentual sobre o
 * subtotal dos produtos); só a porcentagem muda, então ela é um dado do membro,
 * não comportamento próprio.
 */
public enum Regiao {

    SUDESTE("0.01"),
    SUL("0.01"),
    CENTRO_OESTE("0.015"),
    NORTE("0.025"),
    NORDESTE("0.02");

    private final BigDecimal percentual;

    Regiao(String percentual) {
        this.percentual = new BigDecimal(percentual);
    }

    public BigDecimal seguro(BigDecimal subtotalProdutos) {
        return Dinheiro.centavos(subtotalProdutos.multiply(percentual));
    }

    public static Optional<Regiao> resolver(String codigo) {
        return Catalogo.achar(Regiao.class, codigo);
    }
}
