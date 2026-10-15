package br.tcc.checkout.service;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import br.tcc.checkout.dto.CheckoutRequest;
import br.tcc.checkout.dto.CheckoutResponse;
import br.tcc.checkout.dto.ItemRequest;
import br.tcc.checkout.exception.CheckoutException;

class CheckoutServiceTest {

	private CheckoutService service = new CheckoutService();

	@Test
	void testExemplo1_CamisetaTenisExpressaBemvindo10Pix() {
		ItemRequest camiseta = new ItemRequest("Camiseta", 79.90, 2, 0.30);
		ItemRequest tenis = new ItemRequest("Tênis", 249.90, 1, 1.20);
		List<ItemRequest> itens = Arrays.asList(camiseta, tenis);

		CheckoutRequest request = new CheckoutRequest(itens, "EXPRESSA", "BEMVINDO10", "PIX", 1);

		CheckoutResponse response = service.calcularResumo(request);

		assertEquals(409.70, response.getSubtotalProdutos(), 0.01);
		assertEquals(40.97, response.getDescontoCupom(), 0.01);
		assertEquals(33.10, response.getFrete(), 0.01);
		assertEquals(2, response.getPrazoEntregaDias());
		assertEquals(-20.09, response.getAjustePagamento(), 0.01);
		assertEquals(381.74, response.getTotalFinal(), 0.01);
		assertEquals(1, response.getParcelas());
		assertEquals(381.74, response.getValorParcela(), 0.01);
	}

	@Test
	void testExemplo2_CamisetaTenisEconomicaCartao6x() {
		ItemRequest camiseta = new ItemRequest("Camiseta", 79.90, 2, 0.30);
		ItemRequest tenis = new ItemRequest("Tênis", 249.90, 1, 1.20);
		List<ItemRequest> itens = Arrays.asList(camiseta, tenis);

		CheckoutRequest request = new CheckoutRequest(itens, "ECONOMICA", null, "CARTAO", 6);

		CheckoutResponse response = service.calcularResumo(request);

		assertEquals(409.70, response.getSubtotalProdutos(), 0.01);
		assertEquals(0.0, response.getDescontoCupom(), 0.01);
		assertEquals(15.60, response.getFrete(), 0.01);
		assertEquals(7, response.getPrazoEntregaDias());
		assertEquals(30.10, response.getAjustePagamento(), 0.01);
		assertEquals(455.40, response.getTotalFinal(), 0.01);
		assertEquals(6, response.getParcelas());
		assertEquals(75.90, response.getValorParcela(), 0.01);
	}

	@Test
	void testExemplo3_FoneMotoboyCupomMenos50Boleto() {
		ItemRequest fone = new ItemRequest("Fone", 199.90, 2, 0.25);
		List<ItemRequest> itens = Arrays.asList(fone);

		CheckoutRequest request = new CheckoutRequest(itens, "MOTOBOY", "MENOS50", "BOLETO", 1);

		CheckoutResponse response = service.calcularResumo(request);

		assertEquals(399.80, response.getSubtotalProdutos(), 0.01);
		assertEquals(50.0, response.getDescontoCupom(), 0.01);
		assertEquals(18.0, response.getFrete(), 0.01);
		assertEquals(0, response.getPrazoEntregaDias());
		assertEquals(3.49, response.getAjustePagamento(), 0.01);
		assertEquals(371.29, response.getTotalFinal(), 0.01);
		assertEquals(1, response.getParcelas());
		assertEquals(371.29, response.getValorParcela(), 0.01);
	}

	@Test
	void testExemplo4_MeiasCamisetaRetiradadLojaCupomLeve3Pague2Cartao3x() {
		ItemRequest meia = new ItemRequest("Meia", 19.90, 7, 0.10);
		ItemRequest camiseta = new ItemRequest("Camiseta", 79.90, 2, 0.30);
		List<ItemRequest> itens = Arrays.asList(meia, camiseta);

		CheckoutRequest request = new CheckoutRequest(itens, "RETIRADA_LOJA", "LEVE3PAGUE2",
				"CARTAO", 3);

		CheckoutResponse response = service.calcularResumo(request);

		assertEquals(299.10, response.getSubtotalProdutos(), 0.01);
		assertEquals(39.80, response.getDescontoCupom(), 0.01);
		assertEquals(0.0, response.getFrete(), 0.01);
		assertEquals(1, response.getPrazoEntregaDias());
		assertEquals(0.0, response.getAjustePagamento(), 0.01);
		assertEquals(259.30, response.getTotalFinal(), 0.01);
		assertEquals(3, response.getParcelas());
		assertEquals(86.43, response.getValorParcela(), 0.01);
	}

	@Test
	void testPedidoInvalido_CarrinhoVazio() {
		List<ItemRequest> itens = Arrays.asList();
		CheckoutRequest request = new CheckoutRequest(itens, "ECONOMICA", null, "PIX", 1);

		CheckoutException exception = assertThrows(CheckoutException.class,
				() -> service.calcularResumo(request));
		assertEquals("PEDIDO_INVALIDO", exception.getCodigoErro());
	}

	@Test
	void testPedidoInvalido_PrecoNegativo() {
		ItemRequest item = new ItemRequest("Item", -10.0, 1, 0.5);
		List<ItemRequest> itens = Arrays.asList(item);
		CheckoutRequest request = new CheckoutRequest(itens, "ECONOMICA", null, "PIX", 1);

		CheckoutException exception = assertThrows(CheckoutException.class,
				() -> service.calcularResumo(request));
		assertEquals("PEDIDO_INVALIDO", exception.getCodigoErro());
	}

	@Test
	void testModalidadeInvalida() {
		ItemRequest item = new ItemRequest("Item", 10.0, 1, 0.5);
		List<ItemRequest> itens = Arrays.asList(item);
		CheckoutRequest request = new CheckoutRequest(itens, "INVALIDA", null, "PIX", 1);

		CheckoutException exception = assertThrows(CheckoutException.class,
				() -> service.calcularResumo(request));
		assertEquals("MODALIDADE_INVALIDA", exception.getCodigoErro());
	}

	@Test
	void testModalidadeIndisponivel_MotoboySobrepeso() {
		ItemRequest item = new ItemRequest("Item", 100.0, 1, 6.0);
		List<ItemRequest> itens = Arrays.asList(item);
		CheckoutRequest request = new CheckoutRequest(itens, "MOTOBOY", null, "PIX", 1);

		CheckoutException exception = assertThrows(CheckoutException.class,
				() -> service.calcularResumo(request));
		assertEquals("MODALIDADE_INDISPONIVEL", exception.getCodigoErro());
	}

	@Test
	void testCupomInvalido() {
		ItemRequest item = new ItemRequest("Item", 10.0, 1, 0.5);
		List<ItemRequest> itens = Arrays.asList(item);
		CheckoutRequest request = new CheckoutRequest(itens, "ECONOMICA", "CUPOMINVALIDO", "PIX", 1);

		CheckoutException exception = assertThrows(CheckoutException.class,
				() -> service.calcularResumo(request));
		assertEquals("CUPOM_INVALIDO", exception.getCodigoErro());
	}

	@Test
	void testCupomNaoAplicavel_Menos50EmCompraAbaixoDe300() {
		ItemRequest item = new ItemRequest("Item", 100.0, 1, 0.5);
		List<ItemRequest> itens = Arrays.asList(item);
		CheckoutRequest request = new CheckoutRequest(itens, "ECONOMICA", "MENOS50", "PIX", 1);

		CheckoutException exception = assertThrows(CheckoutException.class,
				() -> service.calcularResumo(request));
		assertEquals("CUPOM_NAO_APLICAVEL", exception.getCodigoErro());
	}

	@Test
	void testFormaPagamentoInvalida() {
		ItemRequest item = new ItemRequest("Item", 10.0, 1, 0.5);
		List<ItemRequest> itens = Arrays.asList(item);
		CheckoutRequest request = new CheckoutRequest(itens, "ECONOMICA", null, "INVALIDA", 1);

		CheckoutException exception = assertThrows(CheckoutException.class,
				() -> service.calcularResumo(request));
		assertEquals("FORMA_PAGAMENTO_INVALIDA", exception.getCodigoErro());
	}

	@Test
	void testParcelamentoInvalido_PixComMaisDeUmaParcela() {
		ItemRequest item = new ItemRequest("Item", 10.0, 1, 0.5);
		List<ItemRequest> itens = Arrays.asList(item);
		CheckoutRequest request = new CheckoutRequest(itens, "ECONOMICA", null, "PIX", 2);

		CheckoutException exception = assertThrows(CheckoutException.class,
				() -> service.calcularResumo(request));
		assertEquals("PARCELAMENTO_INVALIDO", exception.getCodigoErro());
	}

	@Test
	void testParcelamentoInvalido_BoletoComMaisDeUmaParcela() {
		ItemRequest item = new ItemRequest("Item", 10.0, 1, 0.5);
		List<ItemRequest> itens = Arrays.asList(item);
		CheckoutRequest request = new CheckoutRequest(itens, "ECONOMICA", null, "BOLETO", 2);

		CheckoutException exception = assertThrows(CheckoutException.class,
				() -> service.calcularResumo(request));
		assertEquals("PARCELAMENTO_INVALIDO", exception.getCodigoErro());
	}

	@Test
	void testParcelamentoInvalido_CartaoAcima12x() {
		ItemRequest item = new ItemRequest("Item", 10.0, 1, 0.5);
		List<ItemRequest> itens = Arrays.asList(item);
		CheckoutRequest request = new CheckoutRequest(itens, "ECONOMICA", null, "CARTAO", 13);

		CheckoutException exception = assertThrows(CheckoutException.class,
				() -> service.calcularResumo(request));
		assertEquals("PARCELAMENTO_INVALIDO", exception.getCodigoErro());
	}

	@Test
	void testFormaPagamentoIndisponivel_BoletoAcimaDeReusMil() {
		ItemRequest item = new ItemRequest("Item", 500.0, 3, 0.5);
		List<ItemRequest> itens = Arrays.asList(item);
		CheckoutRequest request = new CheckoutRequest(itens, "ECONOMICA", null, "BOLETO", 1);

		CheckoutException exception = assertThrows(CheckoutException.class,
				() -> service.calcularResumo(request));
		assertEquals("FORMA_PAGAMENTO_INDISPONIVEL", exception.getCodigoErro());
	}
}
