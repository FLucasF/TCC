package br.com.loja.pedidos.cupom;

import java.math.BigDecimal;
import java.util.List;

import br.com.loja.pedidos.checkout.ItemCarrinho;
import br.com.loja.pedidos.entrega.ModalidadeEntrega;

/** O que um cupom pode consultar do pedido. O frete já vem com as vantagens do clube aplicadas. */
public record ContextoCupom(String cpf, List<ItemCarrinho> itens, BigDecimal subtotalProdutos,
        ModalidadeEntrega modalidade, BigDecimal frete) {
}
