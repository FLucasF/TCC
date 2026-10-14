package br.com.loja.pedidos.pedido.eventos;

import br.com.loja.pedidos.pedido.Pedido;

/**
 * O pedido foi cancelado, de qualquer jeito (pelo cliente, por pagamento fora do prazo ou por
 * falta de estoque).
 *
 * @param estornarPagamento se o pagamento estava confirmado e o que ele gerou deve ser desfeito
 *                          (o estoque já foi devolvido)
 */
public record PedidoCancelado(Pedido pedido, boolean estornarPagamento) {
}
