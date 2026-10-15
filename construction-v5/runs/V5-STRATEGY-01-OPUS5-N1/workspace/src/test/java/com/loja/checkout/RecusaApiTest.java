package com.loja.checkout;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class RecusaApiTest {

    @Autowired
    private MockMvc mockMvc;

    @ParameterizedTest(name = "{1}")
    @CsvSource(delimiter = '|', textBlock = """
            {"itens":[],"modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX","nivelClube":"BRONZE","regiao":"SUL"} | PEDIDO_INVALIDO
            {"modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX","nivelClube":"BRONZE","regiao":"SUL"} | PEDIDO_INVALIDO
            {"itens":[{"nome":"X","precoUnitario":10.00,"quantidade":0,"pesoKg":0.10}],"modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX","nivelClube":"BRONZE","regiao":"SUL"} | PEDIDO_INVALIDO
            {"itens":[{"nome":"X","precoUnitario":10.00,"quantidade":1}],"modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX","nivelClube":"BRONZE","regiao":"SUL"} | PEDIDO_INVALIDO
            {"itens":[{"nome":"X","precoUnitario":-1.00,"quantidade":1,"pesoKg":0.10}],"modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX","nivelClube":"DIAMANTE","regiao":"SUL"} | PEDIDO_INVALIDO
            {"itens":[{"nome":"X","precoUnitario":10.00,"quantidade":1,"pesoKg":0.10}],"modalidadeEntrega":"NAVIO","formaPagamento":"CHEQUE","nivelClube":"DIAMANTE","regiao":"MARTE"} | NIVEL_CLUBE_INVALIDO
            {"itens":[{"nome":"X","precoUnitario":10.00,"quantidade":1,"pesoKg":0.10}],"modalidadeEntrega":"NAVIO","formaPagamento":"CHEQUE","nivelClube":"BRONZE","regiao":"MARTE"} | REGIAO_INVALIDA
            {"itens":[{"nome":"X","precoUnitario":10.00,"quantidade":1,"pesoKg":0.10}],"modalidadeEntrega":"NAVIO","formaPagamento":"CHEQUE","nivelClube":"BRONZE","regiao":"SUL"} | MODALIDADE_INVALIDA
            {"itens":[{"nome":"X","precoUnitario":10.00,"quantidade":1,"pesoKg":0.10}],"formaPagamento":"PIX","nivelClube":"BRONZE","regiao":"SUL"} | MODALIDADE_INVALIDA
            {"itens":[{"nome":"X","precoUnitario":10.00,"quantidade":2,"pesoKg":3.00}],"modalidadeEntrega":"MOTOBOY","cupom":"NAOEXISTE","formaPagamento":"PIX","nivelClube":"BRONZE","regiao":"SUL"} | MODALIDADE_INDISPONIVEL
            {"itens":[{"nome":"X","precoUnitario":10.00,"quantidade":1,"pesoKg":0.10}],"modalidadeEntrega":"EXPRESSA","cupom":"bemvindo10","formaPagamento":"CHEQUE","nivelClube":"BRONZE","regiao":"SUL"} | CUPOM_INVALIDO
            {"itens":[{"nome":"X","precoUnitario":10.00,"quantidade":1,"pesoKg":0.10}],"modalidadeEntrega":"EXPRESSA","cupom":"MENOS50","formaPagamento":"CHEQUE","nivelClube":"BRONZE","regiao":"SUL"} | CUPOM_NAO_APLICAVEL
            {"itens":[{"nome":"X","precoUnitario":10.00,"quantidade":1,"pesoKg":0.10}],"modalidadeEntrega":"EXPRESSA","formaPagamento":"CHEQUE","parcelas":99,"nivelClube":"BRONZE","regiao":"SUL"} | FORMA_PAGAMENTO_INVALIDA
            {"itens":[{"nome":"X","precoUnitario":10.00,"quantidade":1,"pesoKg":0.10}],"modalidadeEntrega":"EXPRESSA","nivelClube":"BRONZE","regiao":"SUL"} | FORMA_PAGAMENTO_INVALIDA
            {"itens":[{"nome":"X","precoUnitario":10.00,"quantidade":1,"pesoKg":0.10}],"modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX","parcelas":2,"nivelClube":"BRONZE","regiao":"SUL"} | PARCELAMENTO_INVALIDO
            {"itens":[{"nome":"X","precoUnitario":10.00,"quantidade":1,"pesoKg":0.10}],"modalidadeEntrega":"EXPRESSA","formaPagamento":"BOLETO","parcelas":3,"nivelClube":"BRONZE","regiao":"SUL"} | PARCELAMENTO_INVALIDO
            {"itens":[{"nome":"X","precoUnitario":10.00,"quantidade":1,"pesoKg":0.10}],"modalidadeEntrega":"EXPRESSA","formaPagamento":"CARTAO","parcelas":13,"nivelClube":"BRONZE","regiao":"SUL"} | PARCELAMENTO_INVALIDO
            {"itens":[{"nome":"X","precoUnitario":2000.00,"quantidade":1,"pesoKg":0.10}],"modalidadeEntrega":"EXPRESSA","formaPagamento":"BOLETO","nivelClube":"BRONZE","regiao":"SUL"} | FORMA_PAGAMENTO_INDISPONIVEL
            """)
    void recusaComOCodigoCerto(String corpo, String codigo) throws Exception {
        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(jsonPath("$.erro").value(codigo))
                .andExpect(jsonPath("$.totalFinal").doesNotExist());
    }

    @Test
    void corpoIlegivelEPedidoInvalido() throws Exception {
        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(jsonPath("$.erro").value("PEDIDO_INVALIDO"));
    }
}
