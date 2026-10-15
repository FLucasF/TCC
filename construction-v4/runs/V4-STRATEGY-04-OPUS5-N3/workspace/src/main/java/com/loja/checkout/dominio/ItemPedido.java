package com.loja.checkout.dominio;

import com.loja.checkout.comum.Dinheiro;
import java.math.BigDecimal;

/** Um produto do carrinho. Preço, quantidade e peso têm de vir e têm de ser positivos. */
public record ItemPedido(String nome, BigDecimal precoUnitario, int quantidade, BigDecimal pesoKg) {

    /** Única porta de entrada: aqui mora a regra de item válido, inclusive dado ausente. */
    public static ItemPedido de(String nome, BigDecimal precoUnitario, Integer quantidade, BigDecimal pesoKg) {
        if (precoUnitario == null || quantidade == null || pesoKg == null
                || precoUnitario.signum() <= 0 || quantidade <= 0 || pesoKg.signum() <= 0) {
            throw new PedidoRecusadoException(CodigoErro.PEDIDO_INVALIDO);
        }
        return new ItemPedido(nome, precoUnitario, quantidade, pesoKg);
    }

    /** Valor do item no carrinho: preço × quantidade. */
    public BigDecimal total() {
        return Dinheiro.centavos(precoUnitario.multiply(BigDecimal.valueOf(quantidade)));
    }

    /** Peso do item no carrinho: peso × quantidade, sem arredondar. */
    public BigDecimal peso() {
        return pesoKg.multiply(BigDecimal.valueOf(quantidade));
    }
}
