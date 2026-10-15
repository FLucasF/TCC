package br.tcc.checkout.dominio;

import br.tcc.checkout.api.dto.ItemPedidoRequest;

import java.math.BigDecimal;
import java.util.List;

/**
 * Pedido já consolidado: subtotal já arredondado, mas o peso é mantido sem
 * arredondar para não distorcer o cálculo do frete.
 */
public record DadosPedido(
		List<ItemPedidoRequest> itens,
		BigDecimal subtotalProdutos,
		BigDecimal pesoTotalKg) {
}
