package br.tcc.checkout.aplicacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import br.tcc.checkout.api.ItemRequest;
import br.tcc.checkout.api.ResumoRequest;
import br.tcc.checkout.api.ResumoResponse;
import br.tcc.checkout.cupom.CatalogoCupons;
import br.tcc.checkout.cupom.CupomBemvindo10;
import br.tcc.checkout.cupom.CupomFreteGratis;
import br.tcc.checkout.cupom.CupomLeve3Pague2;
import br.tcc.checkout.cupom.CupomMenos50;
import br.tcc.checkout.dominio.CheckoutException;
import br.tcc.checkout.dominio.ErroCheckout;
import br.tcc.checkout.entrega.CatalogoEntregas;
import br.tcc.checkout.entrega.EntregaEconomica;
import br.tcc.checkout.entrega.EntregaExpressa;
import br.tcc.checkout.entrega.EntregaMotoboy;
import br.tcc.checkout.entrega.RetiradaLoja;
import br.tcc.checkout.pagamento.CatalogoPagamentos;
import br.tcc.checkout.pagamento.PagamentoBoleto;
import br.tcc.checkout.pagamento.PagamentoCartao;
import br.tcc.checkout.pagamento.PagamentoPix;

class CalculadoraResumoTest {

	private static final ItemRequest CAMISETA = item("Camiseta", "79.90", 2, "0.30");
	private static final ItemRequest TENIS = item("Tênis", "249.90", 1, "1.20");

	private final CalculadoraResumo calculadora = new CalculadoraResumo(
			new CatalogoEntregas(List.of(new EntregaEconomica(), new EntregaExpressa(), new RetiradaLoja(),
					new EntregaMotoboy())),
			new CatalogoCupons(List.of(new CupomBemvindo10(), new CupomMenos50(), new CupomFreteGratis(),
					new CupomLeve3Pague2())),
			new CatalogoPagamentos(List.of(new PagamentoPix(), new PagamentoBoleto(), new PagamentoCartao())));

	// --- exemplos conferidos pelo financeiro ---------------------------------

	@Test
	void exemplo1_expressaComBemvindo10NoPix() {
		ResumoResponse resumo = calcular(List.of(CAMISETA, TENIS), "EXPRESSA", "BEMVINDO10", "PIX", null);

		assertResumo(resumo, "409.70", "40.97", "33.10", 2, "-20.09", "381.74", 1, "381.74");
	}

	@Test
	void exemplo2_economicaSemCupomNoCartaoEm6x() {
		ResumoResponse resumo = calcular(List.of(CAMISETA, TENIS), "ECONOMICA", null, "CARTAO", 6);

		assertResumo(resumo, "409.70", "0.00", "15.60", 7, "30.10", "455.40", 6, "75.90");
	}

	@Test
	void exemplo3_motoboyComMenos50NoBoleto() {
		ResumoResponse resumo = calcular(List.of(item("Fone", "199.90", 2, "0.25")), "MOTOBOY", "MENOS50", "BOLETO",
				null);

		assertResumo(resumo, "399.80", "50.00", "18.00", 0, "3.49", "371.29", 1, "371.29");
	}

	@Test
	void exemplo4_retiradaComLeve3Pague2NoCartaoEm3x() {
		ResumoResponse resumo = calcular(List.of(item("Meia", "19.90", 7, "0.10"), CAMISETA), "RETIRADA_LOJA",
				"LEVE3PAGUE2", "CARTAO", 3);

		assertResumo(resumo, "299.10", "39.80", "0.00", 1, "0.00", "259.30", 3, "86.43");
	}

	// --- entrega -------------------------------------------------------------

	@ParameterizedTest
	@CsvSource({
			"ECONOMICA, 15.60, 7",
			"EXPRESSA, 33.10, 2",
			"RETIRADA_LOJA, 0.00, 1",
			"MOTOBOY, 18.00, 0"
	})
	void cobraFreteEPrazoDeCadaModalidade(String modalidade, String frete, int prazo) {
		ResumoResponse resumo = calcular(List.of(CAMISETA, TENIS), modalidade, null, "PIX", 1);

		assertThat(resumo.frete()).isEqualByComparingTo(frete);
		assertThat(resumo.prazoEntregaDias()).isEqualTo(prazo);
	}

	@Test
	void motoboyAtendeExatamente5Kg() {
		ResumoResponse resumo = calcular(List.of(item("Halter", "100.00", 5, "1.00")), "MOTOBOY", null, "PIX", 1);

		assertThat(resumo.frete()).isEqualByComparingTo("18.00");
	}

	@Test
	void motoboyNaoAtendeAcimaDe5Kg() {
		assertErro(() -> calcular(List.of(item("Halter", "100.00", 5, "1.01")), "MOTOBOY", null, "PIX", 1),
				ErroCheckout.MODALIDADE_INDISPONIVEL);
	}

	// --- cupons --------------------------------------------------------------

	@Test
	void freteGratisDescontaExatamenteOFreteQueContinuaNoResumo() {
		ResumoResponse resumo = calcular(List.of(CAMISETA, TENIS), "EXPRESSA", "FRETEGRATIS", "BOLETO", 1);

		assertThat(resumo.frete()).isEqualByComparingTo("33.10");
		assertThat(resumo.descontoCupom()).isEqualByComparingTo("33.10");
		assertThat(resumo.totalFinal()).isEqualByComparingTo("413.19");
	}

	@Test
	void leve3Pague2ContaOsLotesDeCadaItemSeparadamente() {
		ResumoResponse resumo = calcular(List.of(item("Meia", "10.00", 6, "0.10"), item("Boné", "50.00", 2, "0.20")),
				"RETIRADA_LOJA", "LEVE3PAGUE2", "PIX", 1);

		assertThat(resumo.descontoCupom()).isEqualByComparingTo("20.00");
	}

	@Test
	void menos50ValeAPartirDe300EmProdutos() {
		ResumoResponse resumo = calcular(List.of(item("Jaqueta", "300.00", 1, "0.80")), "RETIRADA_LOJA", "MENOS50",
				"PIX", 1);

		assertThat(resumo.descontoCupom()).isEqualByComparingTo("50.00");
	}

	@Test
	void menos50NaoValeAbaixoDe300EmProdutos() {
		assertErro(() -> calcular(List.of(item("Jaqueta", "299.99", 1, "0.80")), "RETIRADA_LOJA", "MENOS50", "PIX", 1),
				ErroCheckout.CUPOM_NAO_APLICAVEL);
	}

	@Test
	void cupomDesconhecidoEInvalido() {
		assertErro(() -> calcular(List.of(CAMISETA), "RETIRADA_LOJA", "PROMO404", "PIX", 1),
				ErroCheckout.CUPOM_INVALIDO);
	}

	@Test
	void codigoDeCupomEmMinusculasNaoVale() {
		assertErro(() -> calcular(List.of(CAMISETA), "RETIRADA_LOJA", "bemvindo10", "PIX", 1),
				ErroCheckout.CUPOM_INVALIDO);
	}

	@Test
	void pedidoSemCupomNaoTemDesconto() {
		assertThat(calcular(List.of(CAMISETA), "RETIRADA_LOJA", null, "PIX", 1).descontoCupom())
				.isEqualByComparingTo("0.00");
	}

	// --- pagamento -----------------------------------------------------------

	@Test
	void pixDa5PorCentoDeDescontoNoTotalDoPedido() {
		ResumoResponse resumo = calcular(List.of(item("Bolsa", "200.00", 1, "0.50")), "RETIRADA_LOJA", null, "PIX", 1);

		assertThat(resumo.ajustePagamento()).isEqualByComparingTo("-10.00");
		assertThat(resumo.totalFinal()).isEqualByComparingTo("190.00");
	}

	@ParameterizedTest
	@ValueSource(ints = { 1, 2, 3 })
	void cartaoAteTresVezesNaoTemJuros(int parcelas) {
		ResumoResponse resumo = calcular(List.of(item("Bolsa", "300.00", 1, "0.50")), "RETIRADA_LOJA", null, "CARTAO",
				parcelas);

		assertThat(resumo.ajustePagamento()).isEqualByComparingTo("0.00");
		assertThat(resumo.totalFinal()).isEqualByComparingTo("300.00");
		assertThat(resumo.parcelas()).isEqualTo(parcelas);
	}

	@Test
	void cartaoAcimaDeTresVezesCobraJurosPelaTabelaPrice() {
		ResumoResponse resumo = calcular(List.of(item("Bolsa", "1000.00", 1, "0.50")), "RETIRADA_LOJA", null, "CARTAO",
				12);

		// 1000 × 0,0199 ÷ (1 − 1,0199^−12) = 94,4993... → 94,50 × 12
		assertThat(resumo.valorParcela()).isEqualByComparingTo("94.50");
		assertThat(resumo.totalFinal()).isEqualByComparingTo("1134.00");
		assertThat(resumo.ajustePagamento()).isEqualByComparingTo("134.00");
	}

	@Test
	void boletoSomaATarifaBancaria() {
		ResumoResponse resumo = calcular(List.of(item("Bolsa", "200.00", 1, "0.50")), "RETIRADA_LOJA", null, "BOLETO",
				1);

		assertThat(resumo.ajustePagamento()).isEqualByComparingTo("3.49");
		assertThat(resumo.totalFinal()).isEqualByComparingTo("203.49");
	}

	@Test
	void boletoValeAteMilReaisDeTotalDoPedido() {
		assertThat(calcular(List.of(item("Bolsa", "1000.00", 1, "0.50")), "RETIRADA_LOJA", null, "BOLETO", 1)
				.totalFinal()).isEqualByComparingTo("1003.49");
	}

	@Test
	void boletoNaoValeAcimaDeMilReaisDeTotalDoPedido() {
		assertErro(() -> calcular(List.of(item("Bolsa", "1000.01", 1, "0.50")), "RETIRADA_LOJA", null, "BOLETO", 1),
				ErroCheckout.FORMA_PAGAMENTO_INDISPONIVEL);
	}

	@Test
	void oTetoDoBoletoOlhaOTotalDoPedidoJaComCupomEFrete() {
		// 1.040,00 em produtos − 50,00 de cupom + 0,00 de frete = 990,00
		ResumoResponse resumo = calcular(List.of(item("Bolsa", "1040.00", 1, "0.50")), "RETIRADA_LOJA", "MENOS50",
				"BOLETO", 1);

		assertThat(resumo.totalFinal()).isEqualByComparingTo("993.49");
	}

	@ParameterizedTest
	@CsvSource({ "PIX, 2", "BOLETO, 3", "CARTAO, 13", "CARTAO, 0", "CARTAO, -1" })
	void recusaParcelamentoForaDoPermitido(String forma, int parcelas) {
		assertErro(() -> calcular(List.of(CAMISETA), "RETIRADA_LOJA", null, forma, parcelas),
				ErroCheckout.PARCELAMENTO_INVALIDO);
	}

	@Test
	void parcelasAusentesViram1() {
		assertThat(calcular(List.of(CAMISETA), "RETIRADA_LOJA", null, "PIX", null).parcelas()).isEqualTo(1);
	}

	// --- validações e ordem dos erros ---------------------------------------

	@Test
	void recusaPedidoNulo() {
		assertErro(() -> calculadora.calcular(null), ErroCheckout.PEDIDO_INVALIDO);
	}

	@Test
	void recusaCarrinhoVazio() {
		assertErro(() -> calcular(List.of(), "RETIRADA_LOJA", null, "PIX", 1), ErroCheckout.PEDIDO_INVALIDO);
		assertErro(() -> calcular(null, "RETIRADA_LOJA", null, "PIX", 1), ErroCheckout.PEDIDO_INVALIDO);
	}

	@ParameterizedTest
	@CsvSource(nullValues = "nulo", value = {
			"nulo, 1, 0.30",
			"0.00, 1, 0.30",
			"-1.00, 1, 0.30",
			"79.90, 0, 0.30",
			"79.90, -2, 0.30",
			"79.90, 1, nulo",
			"79.90, 1, 0.00",
			"79.90, 1, -0.30"
	})
	void recusaItemComPrecoQuantidadeOuPesoInvalido(String preco, Integer quantidade, String peso) {
		ItemRequest invalido = new ItemRequest("X", preco == null ? null : new BigDecimal(preco), quantidade,
				peso == null ? null : new BigDecimal(peso));

		assertErro(() -> calcular(Arrays.asList(CAMISETA, invalido), "RETIRADA_LOJA", null, "PIX", 1),
				ErroCheckout.PEDIDO_INVALIDO);
	}

	@Test
	void recusaItemComQuantidadeAusente() {
		ItemRequest invalido = new ItemRequest("X", new BigDecimal("10.00"), null, new BigDecimal("0.10"));

		assertErro(() -> calcular(List.of(invalido), "RETIRADA_LOJA", null, "PIX", 1), ErroCheckout.PEDIDO_INVALIDO);
	}

	@Test
	void recusaModalidadeDesconhecidaOuAusente() {
		assertErro(() -> calcular(List.of(CAMISETA), "DRONE", null, "PIX", 1), ErroCheckout.MODALIDADE_INVALIDA);
		assertErro(() -> calcular(List.of(CAMISETA), null, null, "PIX", 1), ErroCheckout.MODALIDADE_INVALIDA);
	}

	@Test
	void recusaFormaDePagamentoDesconhecidaOuAusente() {
		assertErro(() -> calcular(List.of(CAMISETA), "RETIRADA_LOJA", null, "CRIPTO", 1),
				ErroCheckout.FORMA_PAGAMENTO_INVALIDA);
		assertErro(() -> calcular(List.of(CAMISETA), "RETIRADA_LOJA", null, null, 1),
				ErroCheckout.FORMA_PAGAMENTO_INVALIDA);
	}

	@Test
	void pedidoInvalidoVemAntesDeModalidadeInvalida() {
		assertErro(() -> calcular(List.of(), "DRONE", "PROMO404", "CRIPTO", 99), ErroCheckout.PEDIDO_INVALIDO);
	}

	@Test
	void modalidadeInvalidaVemAntesDeModalidadeIndisponivel() {
		assertErro(() -> calcular(List.of(item("Halter", "100.00", 10, "1.00")), "DRONE", null, "PIX", 1),
				ErroCheckout.MODALIDADE_INVALIDA);
	}

	@Test
	void modalidadeIndisponivelVemAntesDeCupomInvalido() {
		assertErro(() -> calcular(List.of(item("Halter", "100.00", 10, "1.00")), "MOTOBOY", "PROMO404", "PIX", 1),
				ErroCheckout.MODALIDADE_INDISPONIVEL);
	}

	@Test
	void cupomInvalidoVemAntesDeFormaDePagamentoInvalida() {
		assertErro(() -> calcular(List.of(CAMISETA), "RETIRADA_LOJA", "PROMO404", "CRIPTO", 1),
				ErroCheckout.CUPOM_INVALIDO);
	}

	@Test
	void cupomNaoAplicavelVemAntesDeFormaDePagamentoInvalida() {
		assertErro(() -> calcular(List.of(CAMISETA), "RETIRADA_LOJA", "MENOS50", "CRIPTO", 1),
				ErroCheckout.CUPOM_NAO_APLICAVEL);
	}

	@Test
	void formaDePagamentoInvalidaVemAntesDeParcelamentoInvalido() {
		assertErro(() -> calcular(List.of(CAMISETA), "RETIRADA_LOJA", null, "CRIPTO", 99),
				ErroCheckout.FORMA_PAGAMENTO_INVALIDA);
	}

	@Test
	void parcelamentoInvalidoVemAntesDeFormaDePagamentoIndisponivel() {
		assertErro(() -> calcular(List.of(item("Bolsa", "2000.00", 1, "0.50")), "RETIRADA_LOJA", null, "BOLETO", 2),
				ErroCheckout.PARCELAMENTO_INVALIDO);
	}

	// --- helpers -------------------------------------------------------------

	private ResumoResponse calcular(List<ItemRequest> itens, String modalidade, String cupom, String pagamento,
			Integer parcelas) {
		return calculadora.calcular(new ResumoRequest(itens, modalidade, cupom, pagamento, parcelas));
	}

	private static ItemRequest item(String nome, String preco, int quantidade, String peso) {
		return new ItemRequest(nome, new BigDecimal(preco), quantidade, new BigDecimal(peso));
	}

	private static void assertResumo(ResumoResponse resumo, String subtotal, String cupom, String frete, int prazo,
			String ajuste, String totalFinal, int parcelas, String valorParcela) {
		assertThat(resumo.subtotalProdutos()).isEqualByComparingTo(subtotal);
		assertThat(resumo.descontoCupom()).isEqualByComparingTo(cupom);
		assertThat(resumo.frete()).isEqualByComparingTo(frete);
		assertThat(resumo.prazoEntregaDias()).isEqualTo(prazo);
		assertThat(resumo.ajustePagamento()).isEqualByComparingTo(ajuste);
		assertThat(resumo.totalFinal()).isEqualByComparingTo(totalFinal);
		assertThat(resumo.parcelas()).isEqualTo(parcelas);
		assertThat(resumo.valorParcela()).isEqualByComparingTo(valorParcela);
	}

	private static void assertErro(Runnable acao, ErroCheckout esperado) {
		assertThatThrownBy(acao::run)
				.isInstanceOf(CheckoutException.class)
				.extracting(excecao -> ((CheckoutException) excecao).getErro())
				.isEqualTo(esperado);
	}
}
