package com.loja.checkout;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
class CheckoutTest {

    private static final String CAMISETA = "{\"nome\":\"Camiseta\",\"precoUnitario\":79.90,\"quantidade\":2,\"pesoKg\":0.30}";
    private static final String TENIS = "{\"nome\":\"Tenis\",\"precoUnitario\":249.90,\"quantidade\":1,\"pesoKg\":1.20}";

    @Autowired
    WebApplicationContext contexto;

    MockMvc mvc() {
        return MockMvcBuilders.webAppContextSetup(contexto).build();
    }

    static String pedido(String itens, String resto) {
        return "{\"itens\":[" + itens + "]," + resto + "}";
    }

    static Stream<Arguments> exemplos() {
        String fone = "{\"nome\":\"Fone\",\"precoUnitario\":199.90,\"quantidade\":2,\"pesoKg\":0.25}";
        String meia = "{\"nome\":\"Meia\",\"precoUnitario\":19.90,\"quantidade\":7,\"pesoKg\":0.10}";
        String cs = CAMISETA + "," + TENIS;
        return Stream.of(
                Arguments.of(pedido(cs, "\"modalidadeEntrega\":\"EXPRESSA\",\"cupom\":\"BEMVINDO10\",\"formaPagamento\":\"PIX\",\"nivelClube\":\"BRONZE\",\"regiao\":\"NORTE\""),
                        "409.70,40.97,33.10,2,10.24,-20.60,391.47,1,391.47,0.00,false"),
                Arguments.of(pedido(cs, "\"modalidadeEntrega\":\"ECONOMICA\",\"formaPagamento\":\"CARTAO\",\"parcelas\":6,\"nivelClube\":\"PRATA\",\"regiao\":\"CENTRO_OESTE\""),
                        "409.70,0.00,15.60,7,6.15,30.55,462.00,6,77.00,8.19,false"),
                Arguments.of(pedido(fone, "\"modalidadeEntrega\":\"MOTOBOY\",\"cupom\":\"MENOS50\",\"formaPagamento\":\"BOLETO\",\"nivelClube\":\"BRONZE\",\"regiao\":\"NORDESTE\""),
                        "399.80,50.00,18.00,0,8.00,3.49,379.29,1,379.29,0.00,false"),
                Arguments.of(pedido(meia + "," + CAMISETA, "\"modalidadeEntrega\":\"RETIRADA_LOJA\",\"cupom\":\"LEVE3PAGUE2\",\"formaPagamento\":\"CARTAO\",\"parcelas\":3,\"nivelClube\":\"PRATA\",\"regiao\":\"SUL\""),
                        "299.10,39.80,0.00,1,2.99,0.00,262.29,3,87.43,5.98,false"),
                Arguments.of(pedido(cs, "\"modalidadeEntrega\":\"EXPRESSA\",\"formaPagamento\":\"PIX\",\"nivelClube\":\"OURO\",\"regiao\":\"SUDESTE\""),
                        "409.70,0.00,0.00,2,4.10,-20.69,393.11,1,393.11,20.48,false"));
    }

    @ParameterizedTest
    @MethodSource("exemplos")
    void exemplosDoFinanceiro(String corpo, String esperado) throws Exception {
        String[] v = esperado.split(",");
        String[] campos = {"subtotalProdutos", "descontoCupom", "frete", "prazoEntregaDias", "seguro",
                "ajustePagamento", "totalFinal", "parcelas", "valorParcela", "creditoProximaCompra", "brinde"};
        StringBuilder json = new StringBuilder("{");
        for (int i = 0; i < campos.length; i++) {
            json.append(i > 0 ? "," : "").append('"').append(campos[i]).append("\":").append(v[i]);
        }
        json.append('}');
        mvc().perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isOk())
                .andExpect(content().json(json.toString(), true))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"frete\":" + v[2])));
    }

    @Test
    void errosNaOrdemEspecificada() throws Exception {
        String ok = "\"modalidadeEntrega\":\"MOTOBOY\",\"formaPagamento\":\"PIX\",\"nivelClube\":\"BRONZE\",\"regiao\":\"SUL\"";
        erro("{\"itens\":[]," + ok + "}", "PEDIDO_INVALIDO");
        erro(pedido(TENIS, ok.replace("BRONZE", "XX")), "NIVEL_CLUBE_INVALIDO");
        erro(pedido(TENIS, ok.replace("SUL", "XX")), "REGIAO_INVALIDA");
        erro(pedido(TENIS, ok.replace("MOTOBOY", "XX")), "MODALIDADE_INVALIDA");
        String pesado = "{\"nome\":\"Piano\",\"precoUnitario\":10,\"quantidade\":1,\"pesoKg\":6}";
        erro(pedido(pesado, ok), "MODALIDADE_INDISPONIVEL");
        erro(pedido(TENIS, ok + ",\"cupom\":\"NADA\""), "CUPOM_INVALIDO");
        erro(pedido(TENIS, ok + ",\"cupom\":\"MENOS50\""), "CUPOM_NAO_APLICAVEL");
        erro(pedido(TENIS, ok.replace("PIX", "XX")), "FORMA_PAGAMENTO_INVALIDA");
        erro(pedido(TENIS, ok + ",\"parcelas\":2"), "PARCELAMENTO_INVALIDO");
        String caro = "{\"nome\":\"Joia\",\"precoUnitario\":1500,\"quantidade\":1,\"pesoKg\":0.1}";
        erro(pedido(caro, ok.replace("PIX", "BOLETO")), "FORMA_PAGAMENTO_INDISPONIVEL");
    }

    private void erro(String corpo, String codigo) throws Exception {
        mvc().perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value(codigo));
    }
}
