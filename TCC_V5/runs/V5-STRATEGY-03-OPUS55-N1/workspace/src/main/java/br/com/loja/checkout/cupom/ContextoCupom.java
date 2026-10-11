package br.com.loja.checkout.cupom;

import br.com.loja.checkout.dominio.Carrinho;
import java.math.BigDecimal;

/** O que um cupom pode olhar no pedido: os itens e o frete que o cliente pagaria. */
public record ContextoCupom(Carrinho carrinho, BigDecimal frete) {
}
