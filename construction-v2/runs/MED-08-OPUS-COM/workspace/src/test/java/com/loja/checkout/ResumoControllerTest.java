package com.loja.checkout;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ResumoControllerTest {

    private static final String CAMISETA = """
            { "nome": "Camiseta", "precoUnitario": 79.90, "quantidade": 2, "pesoKg": 0.30 }""";
    private static final String TENIS = """
            { "nome": "Tenis", "precoUnitario": 249.90, "quantidade": 1, "pesoKg": 1.20 }""";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void exemplo1_expressa_com_bemvindo10_no_pix() throws Exception {
        resumo("""
                { "itens": [%s, %s], "modalidadeEntrega": "EXPRESSA",
                  "cupom": "BEMVINDO10", "formaPagamento": "PIX", "parcelas": 1 }
                """.formatted(CAMISETA, TENIS))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        { "subtotalProdutos": 409.70, "descontoCupom": 40.97, "frete": 33.10,
                          "prazoEntregaDias": 2, "ajustePagamento": -20.09, "totalFinal": 381.74,
                          "parcelas": 1, "valorParcela": 381.74 }
                        """, JsonCompareMode.STRICT));
    }

    @Test
    void exemplo2_economica_sem_cupom_no_cartao_em_6x() throws Exception {
        resumo("""
                { "itens": [%s, %s], "modalidadeEntrega": "ECONOMICA",
                  "formaPagamento": "CARTAO", "parcelas": 6 }
                """.formatted(CAMISETA, TENIS))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        { "subtotalProdutos": 409.70, "descontoCupom": 0.00, "frete": 15.60,
                          "prazoEntregaDias": 7, "ajustePagamento": 30.10, "totalFinal": 455.40,
                          "parcelas": 6, "valorParcela": 75.90 }
                        """, JsonCompareMode.STRICT));
    }

    @Test
    void exemplo3_motoboy_com_menos50_no_boleto() throws Exception {
        resumo("""
                { "itens": [{ "nome": "Fone", "precoUnitario": 199.90, "quantidade": 2, "pesoKg": 0.25 }],
                  "modalidadeEntrega": "MOTOBOY", "cupom": "MENOS50", "formaPagamento": "BOLETO" }
                """)
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        { "subtotalProdutos": 399.80, "descontoCupom": 50.00, "frete": 18.00,
                          "prazoEntregaDias": 0, "ajustePagamento": 3.49, "totalFinal": 371.29,
                          "parcelas": 1, "valorParcela": 371.29 }
                        """, JsonCompareMode.STRICT));
    }

    @Test
    void exemplo4_retirada_com_leve3pague2_no_cartao_em_3x() throws Exception {
        resumo("""
                { "itens": [{ "nome": "Meia", "precoUnitario": 19.90, "quantidade": 7, "pesoKg": 0.10 }, %s],
                  "modalidadeEntrega": "RETIRADA_LOJA", "cupom": "LEVE3PAGUE2",
                  "formaPagamento": "CARTAO", "parcelas": 3 }
                """.formatted(CAMISETA))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        { "subtotalProdutos": 299.10, "descontoCupom": 39.80, "frete": 0.00,
                          "prazoEntregaDias": 1, "ajustePagamento": 0.00, "totalFinal": 259.30,
                          "parcelas": 3, "valorParcela": 86.43 }
                        """, JsonCompareMode.STRICT));
    }

    @Test
    void fretegratis_mostra_o_frete_e_desconta_o_mesmo_valor() throws Exception {
        resumo("""
                { "itens": [%s], "modalidadeEntrega": "EXPRESSA",
                  "cupom": "FRETEGRATIS", "formaPagamento": "CARTAO", "parcelas": 1 }
                """.formatted(CAMISETA))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        { "subtotalProdutos": 159.80, "descontoCupom": 27.70, "frete": 27.70,
                          "prazoEntregaDias": 2, "ajustePagamento": 0.00, "totalFinal": 159.80,
                          "parcelas": 1, "valorParcela": 159.80 }
                        """, JsonCompareMode.STRICT));
    }

    @Test
    void parcelas_ausentes_valem_1() throws Exception {
        resumo("""
                { "itens": [%s], "modalidadeEntrega": "RETIRADA_LOJA", "formaPagamento": "CARTAO" }
                """.formatted(CAMISETA))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        { "totalFinal": 159.80, "parcelas": 1, "valorParcela": 159.80 }
                        """, JsonCompareMode.LENIENT));
    }

    @Test
    void carrinho_vazio_e_pedido_invalido() throws Exception {
        esperaErro("""
                { "itens": [], "modalidadeEntrega": "NAO_EXISTE", "formaPagamento": "NADA" }
                """, "PEDIDO_INVALIDO");
    }

    @Test
    void item_com_quantidade_zero_e_pedido_invalido() throws Exception {
        esperaErro("""
                { "itens": [{ "nome": "Meia", "precoUnitario": 19.90, "quantidade": 0, "pesoKg": 0.10 }],
                  "modalidadeEntrega": "ECONOMICA", "formaPagamento": "PIX" }
                """, "PEDIDO_INVALIDO");
    }

    @Test
    void item_sem_peso_e_pedido_invalido() throws Exception {
        esperaErro("""
                { "itens": [{ "nome": "Meia", "precoUnitario": 19.90, "quantidade": 2 }],
                  "modalidadeEntrega": "ECONOMICA", "formaPagamento": "PIX" }
                """, "PEDIDO_INVALIDO");
    }

    @Test
    void entrega_que_nao_existe_e_modalidade_invalida() throws Exception {
        esperaErro("""
                { "itens": [%s], "modalidadeEntrega": "DRONE", "cupom": "NAOEXISTE", "formaPagamento": "NADA" }
                """.formatted(CAMISETA), "MODALIDADE_INVALIDA");
    }

    @Test
    void entrega_ausente_e_modalidade_invalida() throws Exception {
        esperaErro("""
                { "itens": [%s], "formaPagamento": "PIX" }
                """.formatted(CAMISETA), "MODALIDADE_INVALIDA");
    }

    @Test
    void motoboy_acima_de_5kg_e_modalidade_indisponivel() throws Exception {
        esperaErro("""
                { "itens": [{ "nome": "Halter", "precoUnitario": 99.90, "quantidade": 3, "pesoKg": 2.00 }],
                  "modalidadeEntrega": "MOTOBOY", "formaPagamento": "PIX" }
                """, "MODALIDADE_INDISPONIVEL");
    }

    @Test
    void motoboy_com_exatos_5kg_e_aceito() throws Exception {
        resumo("""
                { "itens": [{ "nome": "Halter", "precoUnitario": 100.00, "quantidade": 5, "pesoKg": 1.00 }],
                  "modalidadeEntrega": "MOTOBOY", "formaPagamento": "PIX" }
                """)
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        { "frete": 18.00, "totalFinal": 492.10 }
                        """, JsonCompareMode.LENIENT));
    }

    @Test
    void cupom_que_nao_existe_e_cupom_invalido() throws Exception {
        esperaErro("""
                { "itens": [%s], "modalidadeEntrega": "ECONOMICA",
                  "cupom": "bemvindo10", "formaPagamento": "NADA" }
                """.formatted(CAMISETA), "CUPOM_INVALIDO");
    }

    @Test
    void menos50_abaixo_de_300_e_cupom_nao_aplicavel() throws Exception {
        esperaErro("""
                { "itens": [%s], "modalidadeEntrega": "ECONOMICA",
                  "cupom": "MENOS50", "formaPagamento": "PIX" }
                """.formatted(CAMISETA), "CUPOM_NAO_APLICAVEL");
    }

    @Test
    void pagamento_que_nao_existe_e_forma_invalida() throws Exception {
        esperaErro("""
                { "itens": [%s], "modalidadeEntrega": "ECONOMICA",
                  "formaPagamento": "CRIPTO", "parcelas": 99 }
                """.formatted(CAMISETA), "FORMA_PAGAMENTO_INVALIDA");
    }

    @Test
    void pix_parcelado_e_parcelamento_invalido() throws Exception {
        esperaErro("""
                { "itens": [%s], "modalidadeEntrega": "ECONOMICA",
                  "formaPagamento": "PIX", "parcelas": 2 }
                """.formatted(CAMISETA), "PARCELAMENTO_INVALIDO");
    }

    @Test
    void cartao_em_13x_e_parcelamento_invalido() throws Exception {
        esperaErro("""
                { "itens": [%s], "modalidadeEntrega": "ECONOMICA",
                  "formaPagamento": "CARTAO", "parcelas": 13 }
                """.formatted(CAMISETA), "PARCELAMENTO_INVALIDO");
    }

    @Test
    void boleto_acima_de_mil_e_forma_indisponivel() throws Exception {
        esperaErro("""
                { "itens": [{ "nome": "Jaqueta", "precoUnitario": 500.00, "quantidade": 2, "pesoKg": 0.50 }],
                  "modalidadeEntrega": "ECONOMICA", "formaPagamento": "BOLETO" }
                """, "FORMA_PAGAMENTO_INDISPONIVEL");
    }

    @Test
    void boleto_com_total_de_exatos_mil_e_aceito() throws Exception {
        resumo("""
                { "itens": [{ "nome": "Jaqueta", "precoUnitario": 492.00, "quantidade": 2, "pesoKg": 1.00 }],
                  "modalidadeEntrega": "ECONOMICA", "formaPagamento": "BOLETO" }
                """)
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        { "frete": 16.00, "ajustePagamento": 3.49, "totalFinal": 1003.49 }
                        """, JsonCompareMode.LENIENT));
    }

    private org.springframework.test.web.servlet.ResultActions resumo(String corpo) throws Exception {
        return mockMvc.perform(post("/checkout/resumo")
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo));
    }

    private void esperaErro(String corpo, String codigo) throws Exception {
        resumo(corpo)
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"erro\": \"%s\"}".formatted(codigo), JsonCompareMode.STRICT));
    }
}
