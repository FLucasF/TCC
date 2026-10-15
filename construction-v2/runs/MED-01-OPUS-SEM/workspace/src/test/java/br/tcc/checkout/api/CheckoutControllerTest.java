package br.tcc.checkout.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@SpringBootTest
@AutoConfigureMockMvc
class CheckoutControllerTest {

	@Autowired
	private MockMvcTester mockMvc;

	@Test
	void devolveOResumoCombinadoComOSite() {
		MvcTestResult resultado = postResumo("""
				{
				  "itens": [
				    { "nome": "Camiseta", "precoUnitario": 79.90, "quantidade": 2, "pesoKg": 0.30 },
				    { "nome": "Tênis", "precoUnitario": 249.90, "quantidade": 1, "pesoKg": 1.20 }
				  ],
				  "modalidadeEntrega": "EXPRESSA",
				  "cupom": "BEMVINDO10",
				  "formaPagamento": "PIX",
				  "parcelas": 1
				}
				""");

		assertThat(resultado).hasStatusOk().hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON)
				.bodyJson().isStrictlyEqualTo("""
						{
						  "subtotalProdutos": 409.70,
						  "descontoCupom": 40.97,
						  "frete": 33.10,
						  "prazoEntregaDias": 2,
						  "ajustePagamento": -20.09,
						  "totalFinal": 381.74,
						  "parcelas": 1,
						  "valorParcela": 381.74
						}
						""");
	}

	@Test
	void escreveOsValoresEmDinheiroComDuasCasasDecimais() {
		MvcTestResult resultado = postResumo("""
				{
				  "itens": [{ "nome": "Meia", "precoUnitario": 20, "quantidade": 1, "pesoKg": 0.1 }],
				  "modalidadeEntrega": "RETIRADA_LOJA",
				  "formaPagamento": "CARTAO",
				  "parcelas": 2
				}
				""");

		assertThat(resultado).hasStatusOk().body().asString()
				.contains("\"subtotalProdutos\":20.00")
				.contains("\"descontoCupom\":0.00")
				.contains("\"frete\":0.00")
				.contains("\"ajustePagamento\":0.00")
				.contains("\"totalFinal\":20.00")
				.contains("\"valorParcela\":10.00");
	}

	@Test
	void cupomEParcelasSaoOpcionais() {
		MvcTestResult resultado = postResumo("""
				{
				  "itens": [{ "nome": "Meia", "precoUnitario": 19.90, "quantidade": 1, "pesoKg": 0.10 }],
				  "modalidadeEntrega": "RETIRADA_LOJA",
				  "cupom": null,
				  "formaPagamento": "PIX"
				}
				""");

		assertThat(resultado).hasStatusOk().bodyJson().isLenientlyEqualTo("""
				{ "descontoCupom": 0.00, "parcelas": 1, "totalFinal": 18.90 }
				""");
	}

	@Test
	void devolve400ComOCodigoDoErroDeNegocio() {
		MvcTestResult resultado = postResumo("""
				{
				  "itens": [{ "nome": "Halter", "precoUnitario": 100.00, "quantidade": 10, "pesoKg": 1.00 }],
				  "modalidadeEntrega": "MOTOBOY",
				  "formaPagamento": "PIX"
				}
				""");

		assertThat(resultado).hasStatus(HttpStatus.BAD_REQUEST)
				.bodyJson().isStrictlyEqualTo("{\"erro\":\"MODALIDADE_INDISPONIVEL\"}");
	}

	@Test
	void devolve400QuandoOCorpoNaoPodeSerLido() {
		assertThat(postResumo("{ nao é json }")).hasStatus(HttpStatus.BAD_REQUEST)
				.bodyJson().isStrictlyEqualTo("{\"erro\":\"PEDIDO_INVALIDO\"}");
	}

	@Test
	void devolve400QuandoNaoVemCorpo() {
		MvcTestResult resultado = mockMvc.post().uri("/checkout/resumo")
				.contentType(MediaType.APPLICATION_JSON).exchange();

		assertThat(resultado).hasStatus(HttpStatus.BAD_REQUEST)
				.bodyJson().isStrictlyEqualTo("{\"erro\":\"PEDIDO_INVALIDO\"}");
	}

	private MvcTestResult postResumo(String corpo) {
		return mockMvc.post().uri("/checkout/resumo")
				.contentType(MediaType.APPLICATION_JSON)
				.content(corpo)
				.exchange();
	}
}
