package com.loja.checkout;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ResumoCheckoutTest {

    private static final String CAMISETA = "{\"nome\":\"Camiseta\",\"precoUnitario\":79.90,\"quantidade\":2,\"pesoKg\":0.30}";
    private static final String TENIS = "{\"nome\":\"Tênis\",\"precoUnitario\":249.90,\"quantidade\":1,\"pesoKg\":1.20}";

    @Autowired
    private MockMvc mockMvc;

    private org.springframework.test.web.servlet.ResultActions resumo(String corpo) throws Exception {
        return mockMvc.perform(post("/checkout/resumo")
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo));
    }

    private void erro(String corpo, String codigo) throws Exception {
        resumo(corpo).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value(codigo));
    }

    @Test
    void exemplo1_expressa_bemvindo10_pix() throws Exception {
        resumo("""
                {"itens":[%s,%s],"modalidadeEntrega":"EXPRESSA","cupom":"BEMVINDO10","formaPagamento":"PIX","parcelas":1}
                """.formatted(CAMISETA, TENIS))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"subtotalProdutos":409.70,"descontoCupom":40.97,"frete":33.10,"prazoEntregaDias":2,
                         "ajustePagamento":-20.09,"totalFinal":381.74,"parcelas":1,"valorParcela":381.74}
                        """));
    }

    @Test
    void exemplo2_economica_sem_cupom_cartao_6x() throws Exception {
        resumo("""
                {"itens":[%s,%s],"modalidadeEntrega":"ECONOMICA","formaPagamento":"CARTAO","parcelas":6}
                """.formatted(CAMISETA, TENIS))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"subtotalProdutos":409.70,"descontoCupom":0.00,"frete":15.60,"prazoEntregaDias":7,
                         "ajustePagamento":30.10,"totalFinal":455.40,"parcelas":6,"valorParcela":75.90}
                        """));
    }

    @Test
    void exemplo3_motoboy_menos50_boleto() throws Exception {
        resumo("""
                {"itens":[{"nome":"Fone","precoUnitario":199.90,"quantidade":2,"pesoKg":0.25}],
                 "modalidadeEntrega":"MOTOBOY","cupom":"MENOS50","formaPagamento":"BOLETO"}
                """)
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"subtotalProdutos":399.80,"descontoCupom":50.00,"frete":18.00,"prazoEntregaDias":0,
                         "ajustePagamento":3.49,"totalFinal":371.29,"parcelas":1,"valorParcela":371.29}
                        """));
    }

    @Test
    void exemplo4_retirada_leve3pague2_cartao_3x_sem_juros() throws Exception {
        resumo("""
                {"itens":[{"nome":"Meia","precoUnitario":19.90,"quantidade":7,"pesoKg":0.10},%s],
                 "modalidadeEntrega":"RETIRADA_LOJA","cupom":"LEVE3PAGUE2","formaPagamento":"CARTAO","parcelas":3}
                """.formatted(CAMISETA))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"subtotalProdutos":299.10,"descontoCupom":39.80,"frete":0.00,"prazoEntregaDias":1,
                         "ajustePagamento":0.00,"totalFinal":259.30,"parcelas":3,"valorParcela":86.43}
                        """));
    }

    @Test
    void fretegratis_zera_o_frete_no_total_mas_mostra_o_frete() throws Exception {
        resumo("""
                {"itens":[%s],"modalidadeEntrega":"EXPRESSA","cupom":"FRETEGRATIS","formaPagamento":"BOLETO"}
                """.formatted(CAMISETA))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"subtotalProdutos":159.80,"descontoCupom":27.70,"frete":27.70,
                         "totalFinal":163.29,"ajustePagamento":3.49}
                        """));
    }

    @Test
    void arredonda_meio_para_o_par() throws Exception {
        // 5% de 59.70 = 2.985 -> 2.98 (par)
        resumo("""
                {"itens":[{"nome":"Bone","precoUnitario":59.70,"quantidade":1,"pesoKg":0.10}],
                 "modalidadeEntrega":"RETIRADA_LOJA","formaPagamento":"PIX"}
                """)
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"subtotalProdutos":59.70,"ajustePagamento":-2.98,"totalFinal":56.72}
                        """));
        // 5% de 59.90 = 2.995 -> 3.00 (par)
        resumo("""
                {"itens":[{"nome":"Bone","precoUnitario":59.90,"quantidade":1,"pesoKg":0.10}],
                 "modalidadeEntrega":"RETIRADA_LOJA","formaPagamento":"PIX"}
                """)
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"subtotalProdutos":59.90,"ajustePagamento":-3.00,"totalFinal":56.90}
                        """));
    }

    @Test
    void carrinho_vazio_ou_item_invalido() throws Exception {
        erro("{\"itens\":[],\"modalidadeEntrega\":\"EXPRESSA\",\"formaPagamento\":\"PIX\"}", "PEDIDO_INVALIDO");
        erro("{\"modalidadeEntrega\":\"EXPRESSA\",\"formaPagamento\":\"PIX\"}", "PEDIDO_INVALIDO");
        erro("""
                {"itens":[{"nome":"X","precoUnitario":0,"quantidade":1,"pesoKg":0.1}],
                 "modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX"}
                """, "PEDIDO_INVALIDO");
        erro("""
                {"itens":[{"nome":"X","precoUnitario":10,"quantidade":0,"pesoKg":0.1}],
                 "modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX"}
                """, "PEDIDO_INVALIDO");
        erro("""
                {"itens":[{"nome":"X","precoUnitario":10,"quantidade":1}],
                 "modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX"}
                """, "PEDIDO_INVALIDO");
    }

    @Test
    void modalidade_invalida_ou_ausente() throws Exception {
        erro("{\"itens\":[%s],\"modalidadeEntrega\":\"DRONE\",\"formaPagamento\":\"PIX\"}".formatted(CAMISETA),
                "MODALIDADE_INVALIDA");
        erro("{\"itens\":[%s],\"formaPagamento\":\"PIX\"}".formatted(CAMISETA), "MODALIDADE_INVALIDA");
        erro("{\"itens\":[%s],\"modalidadeEntrega\":\"expressa\",\"formaPagamento\":\"PIX\"}".formatted(CAMISETA),
                "MODALIDADE_INVALIDA");
    }

    @Test
    void motoboy_acima_de_cinco_quilos() throws Exception {
        erro("""
                {"itens":[{"nome":"Halter","precoUnitario":100.00,"quantidade":2,"pesoKg":3}],
                 "modalidadeEntrega":"MOTOBOY","formaPagamento":"PIX"}
                """, "MODALIDADE_INDISPONIVEL");
    }

    @Test
    void motoboy_leva_exatamente_cinco_quilos() throws Exception {
        resumo("""
                {"itens":[{"nome":"Halter","precoUnitario":100.00,"quantidade":1,"pesoKg":5}],
                 "modalidadeEntrega":"MOTOBOY","formaPagamento":"PIX"}
                """).andExpect(status().isOk()).andExpect(jsonPath("$.frete").value(18.00));
    }

    @Test
    void cupom_inexistente_e_cupom_nao_aplicavel() throws Exception {
        erro("{\"itens\":[%s],\"modalidadeEntrega\":\"EXPRESSA\",\"cupom\":\"NATAL99\",\"formaPagamento\":\"PIX\"}"
                .formatted(CAMISETA), "CUPOM_INVALIDO");
        erro("{\"itens\":[%s],\"modalidadeEntrega\":\"EXPRESSA\",\"cupom\":\"bemvindo10\",\"formaPagamento\":\"PIX\"}"
                .formatted(CAMISETA), "CUPOM_INVALIDO");
        erro("{\"itens\":[%s],\"modalidadeEntrega\":\"EXPRESSA\",\"cupom\":\"MENOS50\",\"formaPagamento\":\"PIX\"}"
                .formatted(CAMISETA), "CUPOM_NAO_APLICAVEL");
    }

    @Test
    void forma_de_pagamento_invalida_ou_ausente() throws Exception {
        erro("{\"itens\":[%s],\"modalidadeEntrega\":\"EXPRESSA\",\"formaPagamento\":\"CRIPTO\"}".formatted(CAMISETA),
                "FORMA_PAGAMENTO_INVALIDA");
        erro("{\"itens\":[%s],\"modalidadeEntrega\":\"EXPRESSA\"}".formatted(CAMISETA), "FORMA_PAGAMENTO_INVALIDA");
    }

    @Test
    void parcelamento_invalido() throws Exception {
        erro("{\"itens\":[%s],\"modalidadeEntrega\":\"EXPRESSA\",\"formaPagamento\":\"PIX\",\"parcelas\":2}"
                .formatted(CAMISETA), "PARCELAMENTO_INVALIDO");
        erro("{\"itens\":[%s],\"modalidadeEntrega\":\"EXPRESSA\",\"formaPagamento\":\"BOLETO\",\"parcelas\":3}"
                .formatted(CAMISETA), "PARCELAMENTO_INVALIDO");
        erro("{\"itens\":[%s],\"modalidadeEntrega\":\"EXPRESSA\",\"formaPagamento\":\"CARTAO\",\"parcelas\":13}"
                .formatted(CAMISETA), "PARCELAMENTO_INVALIDO");
        erro("{\"itens\":[%s],\"modalidadeEntrega\":\"EXPRESSA\",\"formaPagamento\":\"CARTAO\",\"parcelas\":0}"
                .formatted(CAMISETA), "PARCELAMENTO_INVALIDO");
    }

    @Test
    void boleto_acima_de_mil_reais() throws Exception {
        erro("""
                {"itens":[{"nome":"Jaqueta","precoUnitario":500.00,"quantidade":2,"pesoKg":1}],
                 "modalidadeEntrega":"MOTOBOY","formaPagamento":"BOLETO"}
                """, "FORMA_PAGAMENTO_INDISPONIVEL");
    }

    @Test
    void boleto_aceita_exatamente_mil_reais() throws Exception {
        resumo("""
                {"itens":[{"nome":"Jaqueta","precoUnitario":500.00,"quantidade":2,"pesoKg":1}],
                 "modalidadeEntrega":"RETIRADA_LOJA","formaPagamento":"BOLETO"}
                """).andExpect(status().isOk()).andExpect(jsonPath("$.totalFinal").value(1003.49));
    }

    @Test
    void a_ordem_dos_erros_e_respeitada() throws Exception {
        // pedido inválido ganha de modalidade, cupom e pagamento inválidos
        erro("{\"itens\":[],\"modalidadeEntrega\":\"DRONE\",\"cupom\":\"X\",\"formaPagamento\":\"Y\"}",
                "PEDIDO_INVALIDO");
        // modalidade indisponível ganha de cupom inválido
        erro("""
                {"itens":[{"nome":"Halter","precoUnitario":100.00,"quantidade":2,"pesoKg":3}],
                 "modalidadeEntrega":"MOTOBOY","cupom":"X","formaPagamento":"Y"}
                """, "MODALIDADE_INDISPONIVEL");
        // cupom não aplicável ganha de forma de pagamento inválida
        erro("{\"itens\":[%s],\"modalidadeEntrega\":\"EXPRESSA\",\"cupom\":\"MENOS50\",\"formaPagamento\":\"Y\"}"
                .formatted(CAMISETA), "CUPOM_NAO_APLICAVEL");
        // parcelamento inválido ganha de forma de pagamento indisponível
        erro("""
                {"itens":[{"nome":"Jaqueta","precoUnitario":500.00,"quantidade":2,"pesoKg":1}],
                 "modalidadeEntrega":"RETIRADA_LOJA","formaPagamento":"BOLETO","parcelas":2}
                """, "PARCELAMENTO_INVALIDO");
    }
}
