package br.com.loja.pedidos.checkout.entrega;

import br.com.loja.pedidos.checkout.Regiao;
import java.math.BigDecimal;

/** O que uma opção de entrega precisa saber do pedido. */
public record PedidoParaEntrega(String cep, Regiao regiao, BigDecimal pesoKg, BigDecimal subtotalProdutos) {
}
