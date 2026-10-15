package br.tcc.checkout.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import br.tcc.checkout.dto.ItemCarrinho;
import br.tcc.checkout.dto.RequisicaoResumo;
import br.tcc.checkout.dto.RespostaResumo;

public class CheckoutServiceTest {

	private CheckoutService service;

	@BeforeEach
	public void setup() {
		service = new CheckoutService();
	}

	@Test
	public void testExemplo1() {
		RequisicaoResumo req = new RequisicaoResumo();
		req.setItens(Arrays.asList(
			new ItemCarrinho("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
			new ItemCarrinho("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"))
		));
		req.setModalidadeEntrega("EXPRESSA");
		req.setCupom("BEMVINDO10");
		req.setFormaPagamento("PIX");
		req.setParcelas(1);

		assertNull(service.validarRequisicao(req));
		assertNull(service.validarDisponibilidade(req));

		RespostaResumo res = service.calcularResumo(req);
		assertEquals(new BigDecimal("409.70"), res.getSubtotalProdutos());
		assertEquals(new BigDecimal("40.97"), res.getDescontoCupom());
		assertEquals(new BigDecimal("33.10"), res.getFrete());
		assertEquals(2, res.getPrazoEntregaDias());
		assertEquals(new BigDecimal("-20.09"), res.getAjustePagamento());
		assertEquals(new BigDecimal("381.74"), res.getTotalFinal());
		assertEquals(1, res.getParcelas());
		assertEquals(new BigDecimal("381.74"), res.getValorParcela());
	}

	@Test
	public void testExemplo2() {
		RequisicaoResumo req = new RequisicaoResumo();
		req.setItens(Arrays.asList(
			new ItemCarrinho("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
			new ItemCarrinho("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"))
		));
		req.setModalidadeEntrega("ECONOMICA");
		req.setCupom(null);
		req.setFormaPagamento("CARTAO");
		req.setParcelas(6);

		assertNull(service.validarRequisicao(req));
		assertNull(service.validarDisponibilidade(req));

		RespostaResumo res = service.calcularResumo(req);
		assertEquals(new BigDecimal("409.70"), res.getSubtotalProdutos());
		assertEquals(new BigDecimal("0.00"), res.getDescontoCupom());
		assertEquals(new BigDecimal("15.60"), res.getFrete());
		assertEquals(7, res.getPrazoEntregaDias());
		assertEquals(new BigDecimal("30.10"), res.getAjustePagamento());
		assertEquals(new BigDecimal("455.40"), res.getTotalFinal());
		assertEquals(6, res.getParcelas());
		assertEquals(new BigDecimal("75.90"), res.getValorParcela());
	}

	@Test
	public void testExemplo3() {
		RequisicaoResumo req = new RequisicaoResumo();
		req.setItens(Arrays.asList(
			new ItemCarrinho("Fone", new BigDecimal("199.90"), 2, new BigDecimal("0.25"))
		));
		req.setModalidadeEntrega("MOTOBOY");
		req.setCupom("MENOS50");
		req.setFormaPagamento("BOLETO");
		req.setParcelas(1);

		assertNull(service.validarRequisicao(req));
		assertNull(service.validarDisponibilidade(req));

		RespostaResumo res = service.calcularResumo(req);
		assertEquals(new BigDecimal("399.80"), res.getSubtotalProdutos());
		assertEquals(new BigDecimal("50.00"), res.getDescontoCupom());
		assertEquals(new BigDecimal("18.00"), res.getFrete());
		assertEquals(0, res.getPrazoEntregaDias());
		assertEquals(new BigDecimal("3.49"), res.getAjustePagamento());
		assertEquals(new BigDecimal("371.29"), res.getTotalFinal());
		assertEquals(1, res.getParcelas());
		assertEquals(new BigDecimal("371.29"), res.getValorParcela());
	}

	@Test
	public void testExemplo4() {
		RequisicaoResumo req = new RequisicaoResumo();
		req.setItens(Arrays.asList(
			new ItemCarrinho("Meia", new BigDecimal("19.90"), 7, new BigDecimal("0.10")),
			new ItemCarrinho("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30"))
		));
		req.setModalidadeEntrega("RETIRADA_LOJA");
		req.setCupom("LEVE3PAGUE2");
		req.setFormaPagamento("CARTAO");
		req.setParcelas(3);

		assertNull(service.validarRequisicao(req));
		assertNull(service.validarDisponibilidade(req));

		RespostaResumo res = service.calcularResumo(req);
		assertEquals(new BigDecimal("299.10"), res.getSubtotalProdutos());
		assertEquals(new BigDecimal("39.80"), res.getDescontoCupom());
		assertEquals(new BigDecimal("0.00"), res.getFrete());
		assertEquals(1, res.getPrazoEntregaDias());
		assertEquals(new BigDecimal("0.00"), res.getAjustePagamento());
		assertEquals(new BigDecimal("259.30"), res.getTotalFinal());
		assertEquals(3, res.getParcelas());
		assertEquals(new BigDecimal("86.43"), res.getValorParcela());
	}

	@Test
	public void testCarrinhoVazio() {
		RequisicaoResumo req = new RequisicaoResumo();
		req.setItens(Arrays.asList());
		req.setModalidadeEntrega("EXPRESSA");
		req.setFormaPagamento("PIX");

		assertEquals("PEDIDO_INVALIDO", service.validarRequisicao(req));
	}

	@Test
	public void testModalidadeInvalida() {
		RequisicaoResumo req = new RequisicaoResumo();
		req.setItens(Arrays.asList(
			new ItemCarrinho("Camiseta", new BigDecimal("79.90"), 1, new BigDecimal("0.30"))
		));
		req.setModalidadeEntrega("INVALIDA");
		req.setFormaPagamento("PIX");

		assertEquals("MODALIDADE_INVALIDA", service.validarRequisicao(req));
	}

	@Test
	public void testModalidadeIndisponivel() {
		RequisicaoResumo req = new RequisicaoResumo();
		req.setItens(Arrays.asList(
			new ItemCarrinho("Item", new BigDecimal("100.00"), 1, new BigDecimal("10.00"))
		));
		req.setModalidadeEntrega("MOTOBOY");
		req.setFormaPagamento("PIX");

		assertNull(service.validarRequisicao(req));
		assertEquals("MODALIDADE_INDISPONIVEL", service.validarDisponibilidade(req));
	}

	@Test
	public void testCupomInvalido() {
		RequisicaoResumo req = new RequisicaoResumo();
		req.setItens(Arrays.asList(
			new ItemCarrinho("Camiseta", new BigDecimal("79.90"), 1, new BigDecimal("0.30"))
		));
		req.setModalidadeEntrega("EXPRESSA");
		req.setCupom("INVALIDO");
		req.setFormaPagamento("PIX");

		assertEquals("CUPOM_INVALIDO", service.validarRequisicao(req));
	}

	@Test
	public void testCupomNaoAplicavel() {
		RequisicaoResumo req = new RequisicaoResumo();
		req.setItens(Arrays.asList(
			new ItemCarrinho("Camiseta", new BigDecimal("79.90"), 1, new BigDecimal("0.30"))
		));
		req.setModalidadeEntrega("EXPRESSA");
		req.setCupom("MENOS50");
		req.setFormaPagamento("PIX");

		assertEquals("CUPOM_NAO_APLICAVEL", service.validarRequisicao(req));
	}

	@Test
	public void testFormaPagamentoInvalida() {
		RequisicaoResumo req = new RequisicaoResumo();
		req.setItens(Arrays.asList(
			new ItemCarrinho("Camiseta", new BigDecimal("79.90"), 1, new BigDecimal("0.30"))
		));
		req.setModalidadeEntrega("EXPRESSA");
		req.setFormaPagamento("INVALIDA");

		assertEquals("FORMA_PAGAMENTO_INVALIDA", service.validarRequisicao(req));
	}

	@Test
	public void testParcelamentoInvalido() {
		RequisicaoResumo req = new RequisicaoResumo();
		req.setItens(Arrays.asList(
			new ItemCarrinho("Camiseta", new BigDecimal("79.90"), 1, new BigDecimal("0.30"))
		));
		req.setModalidadeEntrega("EXPRESSA");
		req.setFormaPagamento("PIX");
		req.setParcelas(2);

		assertEquals("PARCELAMENTO_INVALIDO", service.validarRequisicao(req));
	}

	@Test
	public void testFormaPagamentoIndisponivel() {
		RequisicaoResumo req = new RequisicaoResumo();
		req.setItens(Arrays.asList(
			new ItemCarrinho("Item", new BigDecimal("500.00"), 3, new BigDecimal("0.50"))
		));
		req.setModalidadeEntrega("EXPRESSA");
		req.setFormaPagamento("BOLETO");
		req.setParcelas(1);

		assertNull(service.validarRequisicao(req));
		assertEquals("FORMA_PAGAMENTO_INDISPONIVEL", service.validarDisponibilidade(req));
	}
}
