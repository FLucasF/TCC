package com.loja.checkout.seguro;

import com.loja.checkout.comum.Dinheiro;

import java.math.BigDecimal;

/**
 * Seguro contra extravio e roubo. O enunciado é explícito: "é só a porcentagem
 * que muda, a conta é a mesma em todas". Então a região é apenas o dado da
 * porcentagem — não há comportamento por região, só a taxa.
 *
 * O seguro é a porcentagem sobre o valor dos produtos (sem desconto, sem frete).
 */
public enum Regiao {

    SUDESTE("1.0"),
    SUL("1.0"),
    CENTRO_OESTE("1.5"),
    NORTE("2.5"),
    NORDESTE("2.0");

    private final BigDecimal percentual;

    Regiao(String percentual) {
        this.percentual = new BigDecimal(percentual);
    }

    public BigDecimal seguro(BigDecimal subtotalProdutos) {
        BigDecimal taxa = percentual.movePointLeft(2);
        return Dinheiro.centavos(subtotalProdutos.multiply(taxa));
    }
}
