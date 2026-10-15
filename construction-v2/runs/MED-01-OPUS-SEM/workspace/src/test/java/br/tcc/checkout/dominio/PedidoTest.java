package br.tcc.checkout.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

class PedidoTest {

	@Test
	void somaProdutosEPesoDosItens() {
		Pedido pedido = new Pedido(List.of(
				new ItemPedido("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
				new ItemPedido("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"))));

		assertThat(pedido.subtotalProdutos()).isEqualByComparingTo("409.70");
		assertThat(pedido.pesoTotalKg()).isEqualByComparingTo("1.80");
	}

	@Test
	void naoArredondaOPesoDoPedido() {
		Pedido pedido = new Pedido(List.of(
				new ItemPedido("Brinco", new BigDecimal("10.00"), 3, new BigDecimal("0.0015"))));

		assertThat(pedido.pesoTotalKg()).isEqualByComparingTo("0.0045");
	}
}
