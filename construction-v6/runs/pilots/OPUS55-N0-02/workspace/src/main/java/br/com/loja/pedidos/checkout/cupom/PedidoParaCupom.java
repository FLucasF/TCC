package br.com.loja.pedidos.checkout.cupom;

import br.com.loja.pedidos.checkout.ItemPrecificado;
import br.com.loja.pedidos.checkout.entrega.ModalidadeEntrega;
import java.math.BigDecimal;
import java.util.List;

/** O que um cupom precisa saber do pedido. O frete já considera as vantagens do clube. */
public record PedidoParaCupom(String cpf, List<ItemPrecificado> itens, BigDecimal subtotalProdutos,
                              ModalidadeEntrega modalidadeEntrega, BigDecimal frete) {
}
