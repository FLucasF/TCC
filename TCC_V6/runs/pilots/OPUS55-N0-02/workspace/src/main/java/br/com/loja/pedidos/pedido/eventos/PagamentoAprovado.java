package br.com.loja.pedidos.pedido.eventos;

import br.com.loja.pedidos.pedido.Pedido;

/**
 * O pagamento do pedido foi confirmado (o estoque já foi baixado). Os interessados reagem com
 * {@code @EventListener}, dentro da mesma transação: se algum falhar, nada é confirmado.
 * Integrações externas (e-mail, SMS, cashback) devem usar {@code @TransactionalEventListener},
 * para só agir depois que o pagamento estiver gravado.
 */
public record PagamentoAprovado(Pedido pedido) {
}
