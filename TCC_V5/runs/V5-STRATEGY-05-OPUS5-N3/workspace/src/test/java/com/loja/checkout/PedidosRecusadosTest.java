package com.loja.checkout;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
class PedidosRecusadosTest {

    private static final String CAMISETA = """
            {"nome":"Camiseta","precoUnitario":79.90,"quantidade":2,"pesoKg":0.30}""";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void carrinho_vazio() throws Exception {
        recusa("""
                {"itens":[],"modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX",
                 "nivelClube":"BRONZE","regiao":"SUDESTE"}
                """, "PEDIDO_INVALIDO");
    }

    @Test
    void item_sem_peso() throws Exception {
        recusa("""
                {"itens":[{"nome":"Camiseta","precoUnitario":79.90,"quantidade":2}],
                 "modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX",
                 "nivelClube":"BRONZE","regiao":"SUDESTE"}
                """, "PEDIDO_INVALIDO");
    }

    @Test
    void item_com_quantidade_zero() throws Exception {
        recusa("""
                {"itens":[{"nome":"Camiseta","precoUnitario":79.90,"quantidade":0,"pesoKg":0.30}],
                 "modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX",
                 "nivelClube":"BRONZE","regiao":"SUDESTE"}
                """, "PEDIDO_INVALIDO");
    }

    @Test
    void item_com_preco_negativo_vem_antes_do_clube_invalido() throws Exception {
        recusa("""
                {"itens":[{"nome":"Camiseta","precoUnitario":-79.90,"quantidade":2,"pesoKg":0.30}],
                 "modalidadeEntrega":"VOANDO","formaPagamento":"CHEQUE",
                 "nivelClube":"DIAMANTE","regiao":"LESTE"}
                """, "PEDIDO_INVALIDO");
    }

    @Test
    void nivel_de_clube_que_nao_existe_vem_antes_da_regiao() throws Exception {
        recusa("""
                {"itens":[%s],"modalidadeEntrega":"VOANDO","formaPagamento":"CHEQUE",
                 "nivelClube":"DIAMANTE","regiao":"LESTE"}
                """.formatted(CAMISETA), "NIVEL_CLUBE_INVALIDO");
    }

    @Test
    void regiao_ausente_vem_antes_da_modalidade() throws Exception {
        recusa("""
                {"itens":[%s],"modalidadeEntrega":"VOANDO","formaPagamento":"CHEQUE",
                 "nivelClube":"BRONZE"}
                """.formatted(CAMISETA), "REGIAO_INVALIDA");
    }

    @Test
    void modalidade_que_nao_existe() throws Exception {
        recusa("""
                {"itens":[%s],"modalidadeEntrega":"VOANDO","formaPagamento":"CHEQUE",
                 "nivelClube":"BRONZE","regiao":"SUDESTE"}
                """.formatted(CAMISETA), "MODALIDADE_INVALIDA");
    }

    @Test
    void motoboy_acima_de_cinco_quilos_vem_antes_do_cupom_invalido() throws Exception {
        recusa("""
                {"itens":[{"nome":"Mala","precoUnitario":500.00,"quantidade":1,"pesoKg":5.01}],
                 "modalidadeEntrega":"MOTOBOY","cupom":"XPTO","formaPagamento":"CHEQUE",
                 "nivelClube":"BRONZE","regiao":"SUDESTE"}
                """, "MODALIDADE_INDISPONIVEL");
    }

    @Test
    void motoboy_com_exatamente_cinco_quilos_e_aceito() throws Exception {
        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"itens":[{"nome":"Mala","precoUnitario":500.00,"quantidade":1,"pesoKg":5.00}],
                         "modalidadeEntrega":"MOTOBOY","formaPagamento":"PIX",
                         "nivelClube":"BRONZE","regiao":"SUDESTE"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.frete").value(18.00));
    }

    @Test
    void cupom_que_nao_existe() throws Exception {
        recusa("""
                {"itens":[%s],"modalidadeEntrega":"EXPRESSA","cupom":"XPTO",
                 "formaPagamento":"CHEQUE","nivelClube":"BRONZE","regiao":"SUDESTE"}
                """.formatted(CAMISETA), "CUPOM_INVALIDO");
    }

    @Test
    void cupom_em_letras_minusculas_nao_existe() throws Exception {
        recusa("""
                {"itens":[%s],"modalidadeEntrega":"EXPRESSA","cupom":"bemvindo10",
                 "formaPagamento":"PIX","nivelClube":"BRONZE","regiao":"SUDESTE"}
                """.formatted(CAMISETA), "CUPOM_INVALIDO");
    }

    @Test
    void menos50_abaixo_de_trezentos_reais() throws Exception {
        recusa("""
                {"itens":[%s],"modalidadeEntrega":"EXPRESSA","cupom":"MENOS50",
                 "formaPagamento":"CHEQUE","nivelClube":"BRONZE","regiao":"SUDESTE"}
                """.formatted(CAMISETA), "CUPOM_NAO_APLICAVEL");
    }

    @Test
    void forma_de_pagamento_que_nao_existe() throws Exception {
        recusa("""
                {"itens":[%s],"modalidadeEntrega":"EXPRESSA","formaPagamento":"CHEQUE",
                 "parcelas":99,"nivelClube":"BRONZE","regiao":"SUDESTE"}
                """.formatted(CAMISETA), "FORMA_PAGAMENTO_INVALIDA");
    }

    @Test
    void pix_parcelado() throws Exception {
        recusa("""
                {"itens":[%s],"modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX",
                 "parcelas":2,"nivelClube":"BRONZE","regiao":"SUDESTE"}
                """.formatted(CAMISETA), "PARCELAMENTO_INVALIDO");
    }

    @Test
    void boleto_parcelado() throws Exception {
        recusa("""
                {"itens":[%s],"modalidadeEntrega":"EXPRESSA","formaPagamento":"BOLETO",
                 "parcelas":3,"nivelClube":"BRONZE","regiao":"SUDESTE"}
                """.formatted(CAMISETA), "PARCELAMENTO_INVALIDO");
    }

    @Test
    void cartao_em_treze_vezes() throws Exception {
        recusa("""
                {"itens":[%s],"modalidadeEntrega":"EXPRESSA","formaPagamento":"CARTAO",
                 "parcelas":13,"nivelClube":"BRONZE","regiao":"SUDESTE"}
                """.formatted(CAMISETA), "PARCELAMENTO_INVALIDO");
    }

    @Test
    void boleto_acima_de_mil_reais() throws Exception {
        recusa("""
                {"itens":[{"nome":"Sofa","precoUnitario":600.00,"quantidade":2,"pesoKg":1.00}],
                 "modalidadeEntrega":"RETIRADA_LOJA","formaPagamento":"BOLETO",
                 "nivelClube":"BRONZE","regiao":"SUDESTE"}
                """, "FORMA_PAGAMENTO_INDISPONIVEL");
    }

    @Test
    void corpo_ilegivel() throws Exception {
        recusa("isso nao e json", "PEDIDO_INVALIDO");
    }

    private void recusa(String corpo, String codigo) throws Exception {
        mockMvc.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value(codigo))
                .andExpect(jsonPath("$.length()").value(1));
    }
}
