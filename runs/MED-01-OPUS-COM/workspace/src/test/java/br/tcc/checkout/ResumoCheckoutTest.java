package br.tcc.checkout;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ResumoCheckoutTest {

	private static final String CAMISETA = "{\"nome\":\"Camiseta\",\"precoUnitario\":79.90,\"quantidade\":2,\"pesoKg\":0.30}";
	private static final String TENIS = "{\"nome\":\"Tenis\",\"precoUnitario\":249.90,\"quantidade\":1,\"pesoKg\":1.20}";

	@Autowired
	private MockMvc mockMvc;

	private org.springframework.test.web.servlet.ResultActions resumo(String corpo) throws Exception {
		return mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(corpo));
	}

	@Test
	void exemplo1_expressa_bemvindo10_pix() throws Exception {
		resumo("""
				{"itens":[%s,%s],"modalidadeEntrega":"EXPRESSA","cupom":"BEMVINDO10","formaPagamento":"PIX","parcelas":1}
				""".formatted(CAMISETA, TENIS))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.subtotalProdutos").value(409.70))
				.andExpect(jsonPath("$.descontoCupom").value(40.97))
				.andExpect(jsonPath("$.frete").value(33.10))
				.andExpect(jsonPath("$.prazoEntregaDias").value(2))
				.andExpect(jsonPath("$.ajustePagamento").value(-20.09))
				.andExpect(jsonPath("$.totalFinal").value(381.74))
				.andExpect(jsonPath("$.parcelas").value(1))
				.andExpect(jsonPath("$.valorParcela").value(381.74));
	}

	@Test
	void exemplo2_economica_sem_cupom_cartao_6x() throws Exception {
		resumo("""
				{"itens":[%s,%s],"modalidadeEntrega":"ECONOMICA","formaPagamento":"CARTAO","parcelas":6}
				""".formatted(CAMISETA, TENIS))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.subtotalProdutos").value(409.70))
				.andExpect(jsonPath("$.descontoCupom").value(0.00))
				.andExpect(jsonPath("$.frete").value(15.60))
				.andExpect(jsonPath("$.prazoEntregaDias").value(7))
				.andExpect(jsonPath("$.ajustePagamento").value(30.10))
				.andExpect(jsonPath("$.totalFinal").value(455.40))
				.andExpect(jsonPath("$.valorParcela").value(75.90));
	}

	@Test
	void exemplo3_motoboy_menos50_boleto() throws Exception {
		resumo("""
				{"itens":[{"nome":"Fone","precoUnitario":199.90,"quantidade":2,"pesoKg":0.25}],
				"modalidadeEntrega":"MOTOBOY","cupom":"MENOS50","formaPagamento":"BOLETO"}
				""")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.subtotalProdutos").value(399.80))
				.andExpect(jsonPath("$.descontoCupom").value(50.00))
				.andExpect(jsonPath("$.frete").value(18.00))
				.andExpect(jsonPath("$.prazoEntregaDias").value(0))
				.andExpect(jsonPath("$.ajustePagamento").value(3.49))
				.andExpect(jsonPath("$.totalFinal").value(371.29))
				.andExpect(jsonPath("$.parcelas").value(1))
				.andExpect(jsonPath("$.valorParcela").value(371.29));
	}

	@Test
	void exemplo4_retirada_leve3pague2_cartao_3x() throws Exception {
		resumo("""
				{"itens":[{"nome":"Meia","precoUnitario":19.90,"quantidade":7,"pesoKg":0.10},%s],
				"modalidadeEntrega":"RETIRADA_LOJA","cupom":"LEVE3PAGUE2","formaPagamento":"CARTAO","parcelas":3}
				""".formatted(CAMISETA))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.subtotalProdutos").value(299.10))
				.andExpect(jsonPath("$.descontoCupom").value(39.80))
				.andExpect(jsonPath("$.frete").value(0.00))
				.andExpect(jsonPath("$.prazoEntregaDias").value(1))
				.andExpect(jsonPath("$.ajustePagamento").value(0.00))
				.andExpect(jsonPath("$.totalFinal").value(259.30))
				.andExpect(jsonPath("$.valorParcela").value(86.43));
	}

	@Test
	void fretegratis_desconta_o_valor_do_frete() throws Exception {
		resumo("""
				{"itens":[%s],"modalidadeEntrega":"EXPRESSA","cupom":"FRETEGRATIS","formaPagamento":"CARTAO"}
				""".formatted(CAMISETA))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.subtotalProdutos").value(159.80))
				.andExpect(jsonPath("$.frete").value(27.70))
				.andExpect(jsonPath("$.descontoCupom").value(27.70))
				.andExpect(jsonPath("$.totalFinal").value(159.80));
	}

	@Test
	void carrinho_vazio_e_item_invalido() throws Exception {
		resumo("{\"itens\":[],\"modalidadeEntrega\":\"EXPRESSA\",\"formaPagamento\":\"PIX\"}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erro").value("PEDIDO_INVALIDO"));
		resumo("""
				{"itens":[{"nome":"X","precoUnitario":10.00,"quantidade":0,"pesoKg":0.10}],
				"modalidadeEntrega":"NAO_EXISTE","formaPagamento":"NADA"}
				""")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erro").value("PEDIDO_INVALIDO"));
	}

	@Test
	void modalidade_invalida_e_indisponivel() throws Exception {
		resumo("""
				{"itens":[%s],"modalidadeEntrega":"DRONE","formaPagamento":"PIX"}
				""".formatted(CAMISETA))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erro").value("MODALIDADE_INVALIDA"));
		resumo("""
				{"itens":[{"nome":"Halter","precoUnitario":50.00,"quantidade":3,"pesoKg":2.00}],
				"modalidadeEntrega":"MOTOBOY","formaPagamento":"PIX"}
				""")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erro").value("MODALIDADE_INDISPONIVEL"));
	}

	@Test
	void cupom_invalido_e_nao_aplicavel() throws Exception {
		resumo("""
				{"itens":[%s],"modalidadeEntrega":"EXPRESSA","cupom":"bemvindo10","formaPagamento":"PIX"}
				""".formatted(CAMISETA))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erro").value("CUPOM_INVALIDO"));
		resumo("""
				{"itens":[%s],"modalidadeEntrega":"EXPRESSA","cupom":"MENOS50","formaPagamento":"PIX"}
				""".formatted(CAMISETA))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erro").value("CUPOM_NAO_APLICAVEL"));
	}

	@Test
	void pagamento_invalido_parcelamento_e_indisponivel() throws Exception {
		resumo("""
				{"itens":[%s],"modalidadeEntrega":"EXPRESSA","formaPagamento":"CHEQUE"}
				""".formatted(CAMISETA))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erro").value("FORMA_PAGAMENTO_INVALIDA"));
		resumo("""
				{"itens":[%s],"modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX","parcelas":2}
				""".formatted(CAMISETA))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erro").value("PARCELAMENTO_INVALIDO"));
		resumo("""
				{"itens":[%s],"modalidadeEntrega":"EXPRESSA","formaPagamento":"CARTAO","parcelas":13}
				""".formatted(CAMISETA))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erro").value("PARCELAMENTO_INVALIDO"));
		resumo("""
				{"itens":[{"nome":"Casaco","precoUnitario":500.00,"quantidade":2,"pesoKg":1.00}],
				"modalidadeEntrega":"EXPRESSA","formaPagamento":"BOLETO"}
				""")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erro").value("FORMA_PAGAMENTO_INDISPONIVEL"));
	}
}
