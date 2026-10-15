package com.loja.checkout;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** Regras que não aparecem nos exemplos do financeiro. */
@SpringBootTest
@AutoConfigureMockMvc
class RegrasDoResumoApiTest {

    private static final String CAMISETA_E_TENIS = """
            {"nome":"Camiseta","precoUnitario":79.90,"quantidade":2,"pesoKg":0.30},
            {"nome":"Tenis","precoUnitario":249.90,"quantidade":1,"pesoKg":1.20}
            """;

    @Autowired
    private MockMvc mockMvc;

    private ResultActions resumo(String pedido) throws Exception {
        return mockMvc.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pedido))
                .andExpect(status().isOk());
    }

    @Test
    void freteGratisMostraOFreteEDescontaOMesmoValor() throws Exception {
        resumo("""
                {"itens":[%s],
                 "modalidadeEntrega":"ECONOMICA","cupom":"FRETEGRATIS",
                 "formaPagamento":"PIX","nivelClube":"BRONZE","regiao":"SUDESTE"}
                """.formatted(CAMISETA_E_TENIS))
                .andExpect(jsonPath("$.frete").value(15.60))
                .andExpect(jsonPath("$.descontoCupom").value(15.60))
                .andExpect(jsonPath("$.totalFinal").value(393.11));
    }

    @Test
    void ouroComFreteGratisNaoTemFreteParaDescontar() throws Exception {
        resumo("""
                {"itens":[%s],
                 "modalidadeEntrega":"ECONOMICA","cupom":"FRETEGRATIS",
                 "formaPagamento":"PIX","nivelClube":"OURO","regiao":"SUDESTE"}
                """.formatted(CAMISETA_E_TENIS))
                .andExpect(jsonPath("$.frete").value(0.00))
                .andExpect(jsonPath("$.descontoCupom").value(0.00))
                .andExpect(jsonPath("$.totalFinal").value(393.11));
    }

    @Test
    void ouroAcimaDeQuinhentosEmProdutosGanhaBrinde() throws Exception {
        resumo("""
                {"itens":[{"nome":"Tenis","precoUnitario":249.90,"quantidade":3,"pesoKg":1.20}],
                 "modalidadeEntrega":"RETIRADA_LOJA",
                 "formaPagamento":"PIX","nivelClube":"OURO","regiao":"SUDESTE"}
                """)
                .andExpect(jsonPath("$.subtotalProdutos").value(749.70))
                .andExpect(jsonPath("$.seguro").value(7.50))
                .andExpect(jsonPath("$.totalFinal").value(719.34))
                .andExpect(jsonPath("$.creditoProximaCompra").value(37.48))
                .andExpect(jsonPath("$.brinde").value(true));
    }

    @Test
    void prataNaoGanhaBrindeNemFreteGratis() throws Exception {
        resumo("""
                {"itens":[{"nome":"Tenis","precoUnitario":249.90,"quantidade":3,"pesoKg":1.20}],
                 "modalidadeEntrega":"ECONOMICA",
                 "formaPagamento":"PIX","nivelClube":"PRATA","regiao":"SUDESTE"}
                """)
                .andExpect(jsonPath("$.frete").value(19.20))
                .andExpect(jsonPath("$.brinde").value(false))
                .andExpect(jsonPath("$.creditoProximaCompra").value(14.99));
    }

    @Test
    void semParcelasInformadasOPedidoEhAVista() throws Exception {
        resumo("""
                {"itens":[%s],
                 "modalidadeEntrega":"RETIRADA_LOJA",
                 "formaPagamento":"CARTAO","nivelClube":"BRONZE","regiao":"SUDESTE"}
                """.formatted(CAMISETA_E_TENIS))
                .andExpect(jsonPath("$.parcelas").value(1))
                .andExpect(jsonPath("$.ajustePagamento").value(0.00))
                .andExpect(jsonPath("$.totalFinal").value(413.80))
                .andExpect(jsonPath("$.valorParcela").value(413.80));
    }

    @Test
    void cartaoEmDozeVezesCobraJurosDaTabelaPrice() throws Exception {
        resumo("""
                {"itens":[%s],
                 "modalidadeEntrega":"RETIRADA_LOJA",
                 "formaPagamento":"CARTAO","parcelas":12,
                 "nivelClube":"BRONZE","regiao":"SUDESTE"}
                """.formatted(CAMISETA_E_TENIS))
                .andExpect(jsonPath("$.parcelas").value(12))
                .andExpect(jsonPath("$.valorParcela").value(39.10))
                .andExpect(jsonPath("$.totalFinal").value(469.20))
                .andExpect(jsonPath("$.ajustePagamento").value(55.40));
    }

    @Test
    void motoboyAtendeExatamenteCincoQuilos() throws Exception {
        resumo("""
                {"itens":[{"nome":"Mala","precoUnitario":100.00,"quantidade":2,"pesoKg":2.50}],
                 "modalidadeEntrega":"MOTOBOY",
                 "formaPagamento":"PIX","nivelClube":"BRONZE","regiao":"SUDESTE"}
                """)
                .andExpect(jsonPath("$.frete").value(18.00))
                .andExpect(jsonPath("$.prazoEntregaDias").value(0));
    }

    @Test
    void menos50ValeExatamenteEmTrezentosReais() throws Exception {
        resumo("""
                {"itens":[{"nome":"Calca","precoUnitario":150.00,"quantidade":2,"pesoKg":0.50}],
                 "modalidadeEntrega":"RETIRADA_LOJA","cupom":"MENOS50",
                 "formaPagamento":"PIX","nivelClube":"BRONZE","regiao":"SUDESTE"}
                """)
                .andExpect(jsonPath("$.descontoCupom").value(50.00))
                .andExpect(jsonPath("$.totalFinal").value(240.35));
    }

    @Test
    void boletoAtendeExatamenteMilReais() throws Exception {
        resumo("""
                {"itens":[{"nome":"Casaco","precoUnitario":488.30,"quantidade":2,"pesoKg":0.50}],
                 "modalidadeEntrega":"RETIRADA_LOJA",
                 "formaPagamento":"BOLETO","nivelClube":"BRONZE","regiao":"SUL"}
                """)
                .andExpect(jsonPath("$.subtotalProdutos").value(976.60))
                .andExpect(jsonPath("$.seguro").value(9.77))
                .andExpect(jsonPath("$.ajustePagamento").value(3.49))
                .andExpect(jsonPath("$.totalFinal").value(989.86));
    }

    @Test
    void seguroSegueAPorcentagemDaRegiao() throws Exception {
        String pedido = """
                {"itens":[{"nome":"Calca","precoUnitario":200.00,"quantidade":1,"pesoKg":0.50}],
                 "modalidadeEntrega":"RETIRADA_LOJA",
                 "formaPagamento":"PIX","nivelClube":"BRONZE","regiao":"%s"}
                """;
        resumo(pedido.formatted("SUDESTE")).andExpect(jsonPath("$.seguro").value(2.00));
        resumo(pedido.formatted("SUL")).andExpect(jsonPath("$.seguro").value(2.00));
        resumo(pedido.formatted("CENTRO_OESTE")).andExpect(jsonPath("$.seguro").value(3.00));
        resumo(pedido.formatted("NORTE")).andExpect(jsonPath("$.seguro").value(5.00));
        resumo(pedido.formatted("NORDESTE")).andExpect(jsonPath("$.seguro").value(4.00));
    }

    @Test
    void leve3Pague2NaoDaDescontoAbaixoDeTresUnidades() throws Exception {
        resumo("""
                {"itens":[{"nome":"Meia","precoUnitario":19.90,"quantidade":2,"pesoKg":0.10}],
                 "modalidadeEntrega":"RETIRADA_LOJA","cupom":"LEVE3PAGUE2",
                 "formaPagamento":"PIX","nivelClube":"BRONZE","regiao":"SUDESTE"}
                """)
                .andExpect(jsonPath("$.descontoCupom").value(0.00));
    }

    @Test
    void leve3Pague2DaDuasUnidadesGratisEmSeisUnidades() throws Exception {
        resumo("""
                {"itens":[{"nome":"Meia","precoUnitario":19.90,"quantidade":6,"pesoKg":0.10}],
                 "modalidadeEntrega":"RETIRADA_LOJA","cupom":"LEVE3PAGUE2",
                 "formaPagamento":"PIX","nivelClube":"BRONZE","regiao":"SUDESTE"}
                """)
                .andExpect(jsonPath("$.descontoCupom").value(39.80));
    }
}
