package br.tcc.checkout.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import br.tcc.checkout.model.ItemCarrinho;
import br.tcc.checkout.model.RequisicaoResumo;
import br.tcc.checkout.model.RespostaResumo;

class ResumoCheckoutServiceTest {

	private ResumoCheckoutService service;

	@BeforeEach
	void setup() {
		service = new ResumoCheckoutService();
	}

	@Test
	void testarExemplo1() {
		List<ItemCarrinho> itens = new ArrayList<>();
		itens.add(new ItemCarrinho("Camiseta", 79.90, 2, 0.30));
		itens.add(new ItemCarrinho("Tênis", 249.90, 1, 1.20));

		RequisicaoResumo requisicao = new RequisicaoResumo(itens, "EXPRESSA", "BEMVINDO10",
				"PIX", 1);

		String erro = service.validarRequisicao(requisicao);
		assertNull(erro);

		RespostaResumo resposta = service.calcularResumo(requisicao);

		assertEquals(409.70, resposta.getSubtotalProdutos());
		assertEquals(40.97, resposta.getDescontoCupom());
		assertEquals(33.10, resposta.getFrete());
		assertEquals(2, resposta.getPrazoEntregaDias());
		assertEquals(-20.09, resposta.getAjustePagamento());
		assertEquals(381.74, resposta.getTotalFinal());
		assertEquals(1, resposta.getParcelas());
		assertEquals(381.74, resposta.getValorParcela());
	}

	@Test
	void testarExemplo2() {
		List<ItemCarrinho> itens = new ArrayList<>();
		itens.add(new ItemCarrinho("Camiseta", 79.90, 2, 0.30));
		itens.add(new ItemCarrinho("Tênis", 249.90, 1, 1.20));

		RequisicaoResumo requisicao = new RequisicaoResumo(itens, "ECONOMICA", null,
				"CARTAO", 6);

		String erro = service.validarRequisicao(requisicao);
		assertNull(erro);

		RespostaResumo resposta = service.calcularResumo(requisicao);

		assertEquals(409.70, resposta.getSubtotalProdutos());
		assertEquals(0.0, resposta.getDescontoCupom());
		assertEquals(15.60, resposta.getFrete());
		assertEquals(7, resposta.getPrazoEntregaDias());
		assertEquals(30.10, resposta.getAjustePagamento());
		assertEquals(455.40, resposta.getTotalFinal());
		assertEquals(6, resposta.getParcelas());
		assertEquals(75.90, resposta.getValorParcela());
	}

	@Test
	void testarExemplo3() {
		List<ItemCarrinho> itens = new ArrayList<>();
		itens.add(new ItemCarrinho("Fone", 199.90, 2, 0.25));

		RequisicaoResumo requisicao = new RequisicaoResumo(itens, "MOTOBOY", "MENOS50",
				"BOLETO", 1);

		String erro = service.validarRequisicao(requisicao);
		assertNull(erro);

		RespostaResumo resposta = service.calcularResumo(requisicao);

		assertEquals(399.80, resposta.getSubtotalProdutos());
		assertEquals(50.0, resposta.getDescontoCupom());
		assertEquals(18.0, resposta.getFrete());
		assertEquals(0, resposta.getPrazoEntregaDias());
		assertEquals(3.49, resposta.getAjustePagamento());
		assertEquals(371.29, resposta.getTotalFinal());
		assertEquals(1, resposta.getParcelas());
		assertEquals(371.29, resposta.getValorParcela());
	}

	@Test
	void testarExemplo4() {
		List<ItemCarrinho> itens = new ArrayList<>();
		itens.add(new ItemCarrinho("Meia", 19.90, 7, 0.10));
		itens.add(new ItemCarrinho("Camiseta", 79.90, 2, 0.30));

		RequisicaoResumo requisicao = new RequisicaoResumo(itens, "RETIRADA_LOJA",
				"LEVE3PAGUE2", "CARTAO", 3);

		String erro = service.validarRequisicao(requisicao);
		assertNull(erro);

		RespostaResumo resposta = service.calcularResumo(requisicao);

		assertEquals(299.10, resposta.getSubtotalProdutos());
		assertEquals(39.80, resposta.getDescontoCupom());
		assertEquals(0.0, resposta.getFrete());
		assertEquals(1, resposta.getPrazoEntregaDias());
		assertEquals(0.0, resposta.getAjustePagamento());
		assertEquals(259.30, resposta.getTotalFinal());
		assertEquals(3, resposta.getParcelas());
		assertEquals(86.43, resposta.getValorParcela());
	}

	@Test
	void testarValidacao_PedidoInvalido_CarrinhoVazio() {
		RequisicaoResumo requisicao = new RequisicaoResumo(new ArrayList<>(), "ECONOMICA",
				null, "PIX", 1);
		String erro = service.validarRequisicao(requisicao);
		assertEquals("PEDIDO_INVALIDO", erro);
	}

	@Test
	void testarValidacao_PedidoInvalido_PrecoZero() {
		List<ItemCarrinho> itens = new ArrayList<>();
		itens.add(new ItemCarrinho("Produto", 0.0, 1, 1.0));

		RequisicaoResumo requisicao = new RequisicaoResumo(itens, "ECONOMICA", null, "PIX", 1);
		String erro = service.validarRequisicao(requisicao);
		assertEquals("PEDIDO_INVALIDO", erro);
	}

	@Test
	void testarValidacao_ModalidadeInvalida() {
		List<ItemCarrinho> itens = new ArrayList<>();
		itens.add(new ItemCarrinho("Produto", 10.0, 1, 1.0));

		RequisicaoResumo requisicao = new RequisicaoResumo(itens, "INVALIDA", null, "PIX", 1);
		String erro = service.validarRequisicao(requisicao);
		assertEquals("MODALIDADE_INVALIDA", erro);
	}

	@Test
	void testarValidacao_ModalidadeIndisponivel_MotoboySobrepesoSobrepesoSobrepeso() {
		List<ItemCarrinho> itens = new ArrayList<>();
		itens.add(new ItemCarrinho("Produto", 10.0, 1, 6.0));

		RequisicaoResumo requisicao = new RequisicaoResumo(itens, "MOTOBOY", null, "PIX", 1);
		String erro = service.validarRequisicao(requisicao);
		assertEquals("MODALIDADE_INDISPONIVEL", erro);
	}

	@Test
	void testarValidacao_CupomInvalido() {
		List<ItemCarrinho> itens = new ArrayList<>();
		itens.add(new ItemCarrinho("Produto", 10.0, 1, 1.0));

		RequisicaoResumo requisicao = new RequisicaoResumo(itens, "ECONOMICA", "INVALIDO",
				"PIX", 1);
		String erro = service.validarRequisicao(requisicao);
		assertEquals("CUPOM_INVALIDO", erro);
	}

	@Test
	void testarValidacao_CupomNaoAplicavel() {
		List<ItemCarrinho> itens = new ArrayList<>();
		itens.add(new ItemCarrinho("Produto", 10.0, 1, 1.0));

		RequisicaoResumo requisicao = new RequisicaoResumo(itens, "ECONOMICA", "MENOS50",
				"PIX", 1);
		String erro = service.validarRequisicao(requisicao);
		assertEquals("CUPOM_NAO_APLICAVEL", erro);
	}

	@Test
	void testarValidacao_FormaPagamentoInvalida() {
		List<ItemCarrinho> itens = new ArrayList<>();
		itens.add(new ItemCarrinho("Produto", 10.0, 1, 1.0));

		RequisicaoResumo requisicao = new RequisicaoResumo(itens, "ECONOMICA", null,
				"INVALIDA", 1);
		String erro = service.validarRequisicao(requisicao);
		assertEquals("FORMA_PAGAMENTO_INVALIDA", erro);
	}

	@Test
	void testarValidacao_ParcelamentoInvalido_Pix() {
		List<ItemCarrinho> itens = new ArrayList<>();
		itens.add(new ItemCarrinho("Produto", 10.0, 1, 1.0));

		RequisicaoResumo requisicao = new RequisicaoResumo(itens, "ECONOMICA", null, "PIX", 2);
		String erro = service.validarRequisicao(requisicao);
		assertEquals("PARCELAMENTO_INVALIDO", erro);
	}

	@Test
	void testarValidacao_ParcelamentoInvalido_CartaoAcima12() {
		List<ItemCarrinho> itens = new ArrayList<>();
		itens.add(new ItemCarrinho("Produto", 10.0, 1, 1.0));

		RequisicaoResumo requisicao = new RequisicaoResumo(itens, "ECONOMICA", null, "CARTAO",
				13);
		String erro = service.validarRequisicao(requisicao);
		assertEquals("PARCELAMENTO_INVALIDO", erro);
	}

	@Test
	void testarValidacao_FormaPagamentoIndisponivel_BoletoSobreLimit() {
		List<ItemCarrinho> itens = new ArrayList<>();
		itens.add(new ItemCarrinho("Produto", 1001.0, 1, 1.0));

		RequisicaoResumo requisicao = new RequisicaoResumo(itens, "RETIRADA_LOJA", null,
				"BOLETO", 1);
		String erro = service.validarRequisicao(requisicao);
		assertEquals("FORMA_PAGAMENTO_INDISPONIVEL", erro);
	}

	@Test
	void testarFretGratis_FRETEGRATIS() {
		List<ItemCarrinho> itens = new ArrayList<>();
		itens.add(new ItemCarrinho("Produto", 100.0, 1, 2.0));

		RequisicaoResumo requisicao = new RequisicaoResumo(itens, "EXPRESSA", "FRETEGRATIS",
				"PIX", 1);

		RespostaResumo resposta = service.calcularResumo(requisicao);
		assertEquals(0.0, resposta.getFrete());
	}

	@Test
	void testarParcelasComJuros() {
		List<ItemCarrinho> itens = new ArrayList<>();
		itens.add(new ItemCarrinho("Produto", 100.0, 1, 1.0));

		RequisicaoResumo requisicao = new RequisicaoResumo(itens, "RETIRADA_LOJA", null,
				"CARTAO", 4);

		RespostaResumo resposta = service.calcularResumo(requisicao);
		assertNotNull(resposta.getAjustePagamento());
		assertTrue(resposta.getAjustePagamento() > 0);
	}

	private void assertTrue(boolean condition) {
		if (!condition) {
			throw new AssertionError();
		}
	}
}
