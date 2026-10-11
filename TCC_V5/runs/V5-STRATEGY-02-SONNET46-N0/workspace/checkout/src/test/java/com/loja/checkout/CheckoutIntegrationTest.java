package com.loja.checkout;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class CheckoutIntegrationTest {

    @Autowired MockMvc mvc;

    private void assertField(String json, String field, double expected) throws Exception {
        mvc.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$." + field).value(expected));
    }

    // ── Exemplo 1 ──────────────────────────────────────────────────────────────
    // Camiseta 79.90×2 + Tênis 249.90×1, EXPRESSA, BEMVINDO10, PIX, BRONZE, NORTE
    static final String EX1 = """
            {"itens":[
              {"nome":"Camiseta","precoUnitario":79.90,"quantidade":2,"pesoKg":0.30},
              {"nome":"Tênis","precoUnitario":249.90,"quantidade":1,"pesoKg":1.20}
            ],
            "modalidadeEntrega":"EXPRESSA","cupom":"BEMVINDO10",
            "formaPagamento":"PIX","parcelas":1,"nivelClube":"BRONZE","regiao":"NORTE"}
            """;

    @Test void exemplo1_subtotal()           throws Exception { assertField(EX1, "subtotalProdutos", 409.70); }
    @Test void exemplo1_descontoCupom()      throws Exception { assertField(EX1, "descontoCupom",    40.97); }
    @Test void exemplo1_frete()              throws Exception { assertField(EX1, "frete",            33.10); }
    @Test void exemplo1_prazo()              throws Exception {
        mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(EX1))
                .andExpect(status().isOk()).andExpect(jsonPath("$.prazoEntregaDias").value(2));
    }
    @Test void exemplo1_seguro()             throws Exception { assertField(EX1, "seguro",           10.24); }
    @Test void exemplo1_ajuste()             throws Exception { assertField(EX1, "ajustePagamento", -20.60); }
    @Test void exemplo1_total()              throws Exception { assertField(EX1, "totalFinal",       391.47); }
    @Test void exemplo1_parcelas()           throws Exception {
        mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(EX1))
                .andExpect(status().isOk()).andExpect(jsonPath("$.parcelas").value(1));
    }
    @Test void exemplo1_valorParcela()       throws Exception { assertField(EX1, "valorParcela",     391.47); }
    @Test void exemplo1_credito()            throws Exception { assertField(EX1, "creditoProximaCompra", 0.00); }
    @Test void exemplo1_brinde()             throws Exception {
        mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(EX1))
                .andExpect(status().isOk()).andExpect(jsonPath("$.brinde").value(false));
    }

    // ── Exemplo 2 ──────────────────────────────────────────────────────────────
    // mesmos itens, ECONOMICA, sem cupom, CARTAO 6x, PRATA, CENTRO_OESTE
    static final String EX2 = """
            {"itens":[
              {"nome":"Camiseta","precoUnitario":79.90,"quantidade":2,"pesoKg":0.30},
              {"nome":"Tênis","precoUnitario":249.90,"quantidade":1,"pesoKg":1.20}
            ],
            "modalidadeEntrega":"ECONOMICA",
            "formaPagamento":"CARTAO","parcelas":6,"nivelClube":"PRATA","regiao":"CENTRO_OESTE"}
            """;

    @Test void exemplo2_subtotal()      throws Exception { assertField(EX2, "subtotalProdutos", 409.70); }
    @Test void exemplo2_descontoCupom() throws Exception { assertField(EX2, "descontoCupom",     0.00); }
    @Test void exemplo2_frete()         throws Exception { assertField(EX2, "frete",             15.60); }
    @Test void exemplo2_prazo()         throws Exception {
        mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(EX2))
                .andExpect(status().isOk()).andExpect(jsonPath("$.prazoEntregaDias").value(7));
    }
    @Test void exemplo2_seguro()        throws Exception { assertField(EX2, "seguro",              6.15); }
    @Test void exemplo2_ajuste()        throws Exception { assertField(EX2, "ajustePagamento",    30.55); }
    @Test void exemplo2_total()         throws Exception { assertField(EX2, "totalFinal",         462.00); }
    @Test void exemplo2_parcelas()      throws Exception {
        mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(EX2))
                .andExpect(status().isOk()).andExpect(jsonPath("$.parcelas").value(6));
    }
    @Test void exemplo2_valorParcela()  throws Exception { assertField(EX2, "valorParcela",        77.00); }
    @Test void exemplo2_credito()       throws Exception { assertField(EX2, "creditoProximaCompra", 8.19); }
    @Test void exemplo2_brinde()        throws Exception {
        mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(EX2))
                .andExpect(status().isOk()).andExpect(jsonPath("$.brinde").value(false));
    }

    // ── Exemplo 3 ──────────────────────────────────────────────────────────────
    // Fone 199.90×2 (0.25kg), MOTOBOY, MENOS50, BOLETO, BRONZE, NORDESTE
    static final String EX3 = """
            {"itens":[
              {"nome":"Fone","precoUnitario":199.90,"quantidade":2,"pesoKg":0.25}
            ],
            "modalidadeEntrega":"MOTOBOY","cupom":"MENOS50",
            "formaPagamento":"BOLETO","parcelas":1,"nivelClube":"BRONZE","regiao":"NORDESTE"}
            """;

    @Test void exemplo3_subtotal()      throws Exception { assertField(EX3, "subtotalProdutos", 399.80); }
    @Test void exemplo3_descontoCupom() throws Exception { assertField(EX3, "descontoCupom",    50.00); }
    @Test void exemplo3_frete()         throws Exception { assertField(EX3, "frete",             18.00); }
    @Test void exemplo3_prazo()         throws Exception {
        mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(EX3))
                .andExpect(status().isOk()).andExpect(jsonPath("$.prazoEntregaDias").value(0));
    }
    @Test void exemplo3_seguro()        throws Exception { assertField(EX3, "seguro",              8.00); }
    @Test void exemplo3_ajuste()        throws Exception { assertField(EX3, "ajustePagamento",     3.49); }
    @Test void exemplo3_total()         throws Exception { assertField(EX3, "totalFinal",         379.29); }
    @Test void exemplo3_valorParcela()  throws Exception { assertField(EX3, "valorParcela",       379.29); }
    @Test void exemplo3_credito()       throws Exception { assertField(EX3, "creditoProximaCompra", 0.00); }

    // ── Exemplo 4 ──────────────────────────────────────────────────────────────
    // Meia 19.90×7 + Camiseta 79.90×2, RETIRADA_LOJA, LEVE3PAGUE2, CARTAO 3x, PRATA, SUL
    static final String EX4 = """
            {"itens":[
              {"nome":"Meia","precoUnitario":19.90,"quantidade":7,"pesoKg":0.10},
              {"nome":"Camiseta","precoUnitario":79.90,"quantidade":2,"pesoKg":0.30}
            ],
            "modalidadeEntrega":"RETIRADA_LOJA","cupom":"LEVE3PAGUE2",
            "formaPagamento":"CARTAO","parcelas":3,"nivelClube":"PRATA","regiao":"SUL"}
            """;

    @Test void exemplo4_subtotal()      throws Exception { assertField(EX4, "subtotalProdutos", 299.10); }
    @Test void exemplo4_descontoCupom() throws Exception { assertField(EX4, "descontoCupom",    39.80); }
    @Test void exemplo4_frete()         throws Exception { assertField(EX4, "frete",              0.00); }
    @Test void exemplo4_prazo()         throws Exception {
        mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(EX4))
                .andExpect(status().isOk()).andExpect(jsonPath("$.prazoEntregaDias").value(1));
    }
    @Test void exemplo4_seguro()        throws Exception { assertField(EX4, "seguro",              2.99); }
    @Test void exemplo4_ajuste()        throws Exception { assertField(EX4, "ajustePagamento",     0.00); }
    @Test void exemplo4_total()         throws Exception { assertField(EX4, "totalFinal",         262.29); }
    @Test void exemplo4_parcelas()      throws Exception {
        mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(EX4))
                .andExpect(status().isOk()).andExpect(jsonPath("$.parcelas").value(3));
    }
    @Test void exemplo4_valorParcela()  throws Exception { assertField(EX4, "valorParcela",        87.43); }
    @Test void exemplo4_credito()       throws Exception { assertField(EX4, "creditoProximaCompra", 5.98); }

    // ── Exemplo 5 ──────────────────────────────────────────────────────────────
    // Camiseta 79.90×2 + Tênis 249.90×1, EXPRESSA, sem cupom, PIX, OURO, SUDESTE
    static final String EX5 = """
            {"itens":[
              {"nome":"Camiseta","precoUnitario":79.90,"quantidade":2,"pesoKg":0.30},
              {"nome":"Tênis","precoUnitario":249.90,"quantidade":1,"pesoKg":1.20}
            ],
            "modalidadeEntrega":"EXPRESSA",
            "formaPagamento":"PIX","parcelas":1,"nivelClube":"OURO","regiao":"SUDESTE"}
            """;

    @Test void exemplo5_subtotal()      throws Exception { assertField(EX5, "subtotalProdutos", 409.70); }
    @Test void exemplo5_descontoCupom() throws Exception { assertField(EX5, "descontoCupom",     0.00); }
    @Test void exemplo5_frete()         throws Exception { assertField(EX5, "frete",              0.00); }
    @Test void exemplo5_seguro()        throws Exception { assertField(EX5, "seguro",              4.10); }
    @Test void exemplo5_ajuste()        throws Exception { assertField(EX5, "ajustePagamento",  -20.69); }
    @Test void exemplo5_total()         throws Exception { assertField(EX5, "totalFinal",        393.11); }
    @Test void exemplo5_valorParcela()  throws Exception { assertField(EX5, "valorParcela",      393.11); }
    @Test void exemplo5_credito()       throws Exception { assertField(EX5, "creditoProximaCompra", 20.48); }
    @Test void exemplo5_brinde()        throws Exception {
        mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(EX5))
                .andExpect(status().isOk()).andExpect(jsonPath("$.brinde").value(false));
    }

    // ── Erros ──────────────────────────────────────────────────────────────────
    @Test void erro_carrinhoVazio() throws Exception {
        String json = """
                {"itens":[],"modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX",
                 "parcelas":1,"nivelClube":"BRONZE","regiao":"SUDESTE"}
                """;
        mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value("PEDIDO_INVALIDO"));
    }

    @Test void erro_nivelClubeInvalido() throws Exception {
        String json = """
                {"itens":[{"nome":"X","precoUnitario":10.0,"quantidade":1,"pesoKg":0.5}],
                 "modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX",
                 "parcelas":1,"nivelClube":"PLATINA","regiao":"SUDESTE"}
                """;
        mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value("NIVEL_CLUBE_INVALIDO"));
    }

    @Test void erro_regiaoInvalida() throws Exception {
        String json = """
                {"itens":[{"nome":"X","precoUnitario":10.0,"quantidade":1,"pesoKg":0.5}],
                 "modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX",
                 "parcelas":1,"nivelClube":"BRONZE","regiao":"LESTE"}
                """;
        mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value("REGIAO_INVALIDA"));
    }

    @Test void erro_modalidadeInvalida() throws Exception {
        String json = """
                {"itens":[{"nome":"X","precoUnitario":10.0,"quantidade":1,"pesoKg":0.5}],
                 "modalidadeEntrega":"DRONE","formaPagamento":"PIX",
                 "parcelas":1,"nivelClube":"BRONZE","regiao":"SUDESTE"}
                """;
        mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value("MODALIDADE_INVALIDA"));
    }

    @Test void erro_motoboyAcima5kg() throws Exception {
        String json = """
                {"itens":[{"nome":"X","precoUnitario":10.0,"quantidade":1,"pesoKg":6.0}],
                 "modalidadeEntrega":"MOTOBOY","formaPagamento":"PIX",
                 "parcelas":1,"nivelClube":"BRONZE","regiao":"SUDESTE"}
                """;
        mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value("MODALIDADE_INDISPONIVEL"));
    }

    @Test void erro_cupomInvalido() throws Exception {
        String json = """
                {"itens":[{"nome":"X","precoUnitario":10.0,"quantidade":1,"pesoKg":0.5}],
                 "modalidadeEntrega":"EXPRESSA","cupom":"DESCONTO99","formaPagamento":"PIX",
                 "parcelas":1,"nivelClube":"BRONZE","regiao":"SUDESTE"}
                """;
        mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value("CUPOM_INVALIDO"));
    }

    @Test void erro_cupomNaoAplicavel() throws Exception {
        String json = """
                {"itens":[{"nome":"X","precoUnitario":10.0,"quantidade":1,"pesoKg":0.5}],
                 "modalidadeEntrega":"EXPRESSA","cupom":"MENOS50","formaPagamento":"PIX",
                 "parcelas":1,"nivelClube":"BRONZE","regiao":"SUDESTE"}
                """;
        mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value("CUPOM_NAO_APLICAVEL"));
    }

    @Test void erro_formaPagamentoInvalida() throws Exception {
        String json = """
                {"itens":[{"nome":"X","precoUnitario":10.0,"quantidade":1,"pesoKg":0.5}],
                 "modalidadeEntrega":"EXPRESSA","formaPagamento":"CRYPTO",
                 "parcelas":1,"nivelClube":"BRONZE","regiao":"SUDESTE"}
                """;
        mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value("FORMA_PAGAMENTO_INVALIDA"));
    }

    @Test void erro_parcelamentoInvalido_pix() throws Exception {
        String json = """
                {"itens":[{"nome":"X","precoUnitario":10.0,"quantidade":1,"pesoKg":0.5}],
                 "modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX",
                 "parcelas":3,"nivelClube":"BRONZE","regiao":"SUDESTE"}
                """;
        mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value("PARCELAMENTO_INVALIDO"));
    }

    @Test void erro_boletoAcima1000() throws Exception {
        String json = """
                {"itens":[{"nome":"X","precoUnitario":600.0,"quantidade":2,"pesoKg":0.5}],
                 "modalidadeEntrega":"EXPRESSA","formaPagamento":"BOLETO",
                 "parcelas":1,"nivelClube":"BRONZE","regiao":"SUDESTE"}
                """;
        mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value("FORMA_PAGAMENTO_INDISPONIVEL"));
    }
}
