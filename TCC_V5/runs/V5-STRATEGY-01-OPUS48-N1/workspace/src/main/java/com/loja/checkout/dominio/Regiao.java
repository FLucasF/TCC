package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Região do cliente. O enunciado diz que só a porcentagem do seguro muda entre
 * as regiões e a conta é a mesma em todas; por isso a região é apenas um dado
 * (o percentual), e não um comportamento próprio de cada caso.
 *
 * O seguro é o percentual sobre o valor dos produtos (sem desconto e sem frete).
 */
public enum Regiao {

    SUDESTE("0.01"),
    SUL("0.01"),
    CENTRO_OESTE("0.015"),
    NORTE("0.025"),
    NORDESTE("0.02");

    private final BigDecimal fracao;

    Regiao(String fracao) {
        this.fracao = new BigDecimal(fracao);
    }

    public BigDecimal seguro(BigDecimal subtotalProdutos) {
        return Dinheiro.centavos(subtotalProdutos.multiply(fracao));
    }

    public static Optional<Regiao> de(String nome) {
        if (nome == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(valueOf(nome));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
