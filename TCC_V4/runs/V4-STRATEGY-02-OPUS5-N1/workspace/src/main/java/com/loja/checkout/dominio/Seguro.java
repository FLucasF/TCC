package com.loja.checkout.dominio;

import java.math.BigDecimal;

/**
 * Seguro contra extravio e roubo, que vai em todo pedido: a porcentagem da
 * regiao sobre o valor dos produtos, sem desconto e sem frete.
 */
public final class Seguro {

    private Seguro() {
    }

    public static BigDecimal doPedido(BigDecimal subtotalProdutos, Regiao regiao) {
        return Dinheiro.percentual(subtotalProdutos, regiao.percentualDoSeguro());
    }
}
