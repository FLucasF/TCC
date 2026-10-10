package com.loja.checkout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** O formato combinado com o site: o endereco, o JSON de ida e o de volta. */
@SpringBootTest
@AutoConfigureMockMvc
class ResumoHttpTest {

    private static final String PEDIDO_DO_ANEXO = """
            {
              "itens": [
                {"nome": "Camiseta", "precoUnitario": 79.90, "quantidade": 2, "pesoKg": 0.30},
                {"nome": "Tenis", "precoUnitario": 249.90, "quantidade": 1, "pesoKg": 1.20}
              ],
              "modalidadeEntrega": "EXPRESSA",
              "cupom": "BEMVINDO10",
              "formaPagamento": "PIX",
              "parcelas": 1,
              "nivelClube": "OURO",
              "regiao": "SUDESTE"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("devolve o resumo com todos os campos e sempre com duas casas decimais")
    void devolveOResumo() throws Exception {
        String corpo = mockMvc.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PEDIDO_DO_ANEXO))
                .andReturn().getResponse().getContentAsString();

        assertThat(corpo).contains(
                "\"subtotalProdutos\":409.70",
                "\"descontoCupom\":40.97",
                "\"frete\":0.00",
                "\"prazoEntregaDias\":2",
                "\"seguro\":4.10",
                "\"ajustePagamento\":-18.64",
                "\"totalFinal\":354.19",
                "\"parcelas\":1",
                "\"valorParcela\":354.19",
                "\"creditoProximaCompra\":20.48",
                "\"brinde\":false");
    }

    @Test
    @DisplayName("responde 200 para o pedido que da certo")
    void respondeComSucesso() throws Exception {
        mockMvc.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PEDIDO_DO_ANEXO))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .status().isOk());
    }

    @Test
    @DisplayName("o pedido sem cupom e sem parcelas tambem e aceito")
    void camposOpcionaisAusentes() throws Exception {
        String corpo = mockMvc.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "itens": [{"nome": "Fone", "precoUnitario": 199.90, "quantidade": 2, "pesoKg": 0.25}],
                                  "modalidadeEntrega": "MOTOBOY",
                                  "formaPagamento": "BOLETO",
                                  "nivelClube": "BRONZE",
                                  "regiao": "NORDESTE"
                                }
                                """))
                .andReturn().getResponse().getContentAsString();

        assertThat(corpo).contains("\"totalFinal\":429.29", "\"parcelas\":1", "\"valorParcela\":429.29");
    }

    @Test
    @DisplayName("quando recusa, devolve so o codigo do problema")
    void devolveSoOCodigo() throws Exception {
        String corpo = mockMvc.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "itens": [{"nome": "Jaqueta", "precoUnitario": 100.00, "quantidade": 6, "pesoKg": 1.00}],
                                  "modalidadeEntrega": "MOTOBOY",
                                  "formaPagamento": "PIX",
                                  "nivelClube": "BRONZE",
                                  "regiao": "SUDESTE"
                                }
                                """))
                .andReturn().getResponse().getContentAsString();

        assertThat(corpo).isEqualTo("{\"erro\":\"MODALIDADE_INDISPONIVEL\"}");
    }

    @Test
    @DisplayName("JSON que nem forma um pedido tambem e recusado")
    void jsonIlegivel() throws Exception {
        String corpo = mockMvc.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itens\": \"uma camiseta\"}"))
                .andReturn().getResponse().getContentAsString();

        assertThat(corpo).isEqualTo("{\"erro\":\"PEDIDO_INVALIDO\"}");
    }
}
