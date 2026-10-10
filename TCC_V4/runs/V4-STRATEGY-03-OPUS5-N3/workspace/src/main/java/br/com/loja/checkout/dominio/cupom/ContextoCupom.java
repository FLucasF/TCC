package br.com.loja.checkout.dominio.cupom;

import java.util.List;

import br.com.loja.checkout.dominio.Dinheiro;
import br.com.loja.checkout.dominio.Item;

/**
 * O que um cupom pode olhar para decidir o desconto: os itens, o valor dos
 * produtos e o frete ja calculado.
 */
public record ContextoCupom(List<Item> itens, Dinheiro subtotalProdutos, Dinheiro frete) {
}
