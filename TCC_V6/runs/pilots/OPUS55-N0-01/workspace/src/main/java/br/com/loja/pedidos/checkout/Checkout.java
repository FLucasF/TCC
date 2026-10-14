package br.com.loja.pedidos.checkout;

import java.util.List;

/** Os dados da compra já conferidos, com o resumo calculado. */
public record Checkout(String cpf, String email, List<ItemCarrinho> itens, Resumo resumo) {
}
