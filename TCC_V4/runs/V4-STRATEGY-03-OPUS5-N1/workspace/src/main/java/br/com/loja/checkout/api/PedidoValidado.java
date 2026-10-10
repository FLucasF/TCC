package br.com.loja.checkout.api;

import br.com.loja.checkout.calculo.Pedido;
import br.com.loja.checkout.pagamento.EscolhaPagamento;

public record PedidoValidado(Pedido pedido, EscolhaPagamento pagamento) {
}
