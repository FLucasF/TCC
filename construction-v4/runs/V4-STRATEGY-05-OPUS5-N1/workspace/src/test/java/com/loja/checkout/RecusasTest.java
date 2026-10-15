package com.loja.checkout;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RecusasTest {

    private static final String ITEM = """
            {"nome":"Camiseta","precoUnitario":79.90,"quantidade":2,"pesoKg":0.30}
            """;

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Carrinho vazio ou ausente")
    void pedidoInvalido() throws Exception {
        recusa("""
                {"itens":[],"modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX",
                 "nivelClube":"BRONZE","regiao":"SUL"}
                """, "PEDIDO_INVALIDO");
        recusa("""
                {"modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX",
                 "nivelClube":"BRONZE","regiao":"SUL"}
                """, "PEDIDO_INVALIDO");
    }

    @Test
    @DisplayName("Item com preco, quantidade ou peso zero, negativo ou ausente")
    void itemInvalido() throws Exception {
        recusa(pedidoCom("""
                {"nome":"Camiseta","precoUnitario":0,"quantidade":2,"pesoKg":0.30}
                """), "PEDIDO_INVALIDO");
        recusa(pedidoCom("""
                {"nome":"Camiseta","precoUnitario":79.90,"quantidade":0,"pesoKg":0.30}
                """), "PEDIDO_INVALIDO");
        recusa(pedidoCom("""
                {"nome":"Camiseta","precoUnitario":79.90,"quantidade":2,"pesoKg":-0.30}
                """), "PEDIDO_INVALIDO");
        recusa(pedidoCom("""
                {"nome":"Camiseta","quantidade":2,"pesoKg":0.30}
                """), "PEDIDO_INVALIDO");
    }

    @Test
    @DisplayName("Pedido invalido vem antes de qualquer outro problema")
    void pedidoInvalidoTemPrioridade() throws Exception {
        recusa("""
                {"itens":[],"modalidadeEntrega":"DRONE","cupom":"NAOEXISTE",
                 "formaPagamento":"CHEQUE","nivelClube":"DIAMANTE","regiao":"LESTE"}
                """, "PEDIDO_INVALIDO");
    }

    @Test
    @DisplayName("Nivel do clube inexistente ou ausente, antes da regiao")
    void nivelClubeInvalido() throws Exception {
        recusa("""
                {"itens":[%s],"modalidadeEntrega":"DRONE","formaPagamento":"CHEQUE",
                 "nivelClube":"DIAMANTE","regiao":"LESTE"}
                """.formatted(ITEM), "NIVEL_CLUBE_INVALIDO");
        recusa("""
                {"itens":[%s],"modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX",
                 "regiao":"SUL"}
                """.formatted(ITEM), "NIVEL_CLUBE_INVALIDO");
    }

    @Test
    @DisplayName("Regiao inexistente ou ausente, antes da modalidade")
    void regiaoInvalida() throws Exception {
        recusa("""
                {"itens":[%s],"modalidadeEntrega":"DRONE","formaPagamento":"CHEQUE",
                 "nivelClube":"BRONZE","regiao":"LESTE"}
                """.formatted(ITEM), "REGIAO_INVALIDA");
        recusa("""
                {"itens":[%s],"modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX",
                 "nivelClube":"BRONZE"}
                """.formatted(ITEM), "REGIAO_INVALIDA");
    }

    @Test
    @DisplayName("Modalidade inexistente ou ausente, antes do cupom")
    void modalidadeInvalida() throws Exception {
        recusa("""
                {"itens":[%s],"modalidadeEntrega":"DRONE","cupom":"NAOEXISTE",
                 "formaPagamento":"CHEQUE","nivelClube":"BRONZE","regiao":"SUL"}
                """.formatted(ITEM), "MODALIDADE_INVALIDA");
        recusa("""
                {"itens":[%s],"formaPagamento":"PIX","nivelClube":"BRONZE","regiao":"SUL"}
                """.formatted(ITEM), "MODALIDADE_INVALIDA");
    }

    @Test
    @DisplayName("Motoboy acima de 5 kg nao esta disponivel")
    void modalidadeIndisponivel() throws Exception {
        recusa("""
                {"itens":[{"nome":"Mala","precoUnitario":100.00,"quantidade":2,"pesoKg":2.6}],
                 "modalidadeEntrega":"MOTOBOY","formaPagamento":"PIX",
                 "nivelClube":"BRONZE","regiao":"SUL"}
                """, "MODALIDADE_INDISPONIVEL");
    }

    @Test
    @DisplayName("Motoboy com exatamente 5 kg esta disponivel")
    void motoboyNoLimite() throws Exception {
        mockMvc.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"itens":[{"nome":"Mala","precoUnitario":100.00,"quantidade":2,"pesoKg":2.5}],
                                 "modalidadeEntrega":"MOTOBOY","formaPagamento":"PIX",
                                 "nivelClube":"BRONZE","regiao":"SUL"}
                                """))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Cupom inexistente, antes da forma de pagamento")
    void cupomInvalido() throws Exception {
        recusa("""
                {"itens":[%s],"modalidadeEntrega":"EXPRESSA","cupom":"bemvindo10",
                 "formaPagamento":"CHEQUE","nivelClube":"BRONZE","regiao":"SUL"}
                """.formatted(ITEM), "CUPOM_INVALIDO");
    }

    @Test
    @DisplayName("MENOS50 abaixo de R$ 300,00 em produtos")
    void cupomNaoAplicavel() throws Exception {
        recusa("""
                {"itens":[%s],"modalidadeEntrega":"EXPRESSA","cupom":"MENOS50",
                 "formaPagamento":"PIX","nivelClube":"BRONZE","regiao":"SUL"}
                """.formatted(ITEM), "CUPOM_NAO_APLICAVEL");
    }

    @Test
    @DisplayName("Forma de pagamento inexistente ou ausente")
    void formaPagamentoInvalida() throws Exception {
        recusa("""
                {"itens":[%s],"modalidadeEntrega":"EXPRESSA","formaPagamento":"CHEQUE",
                 "parcelas":99,"nivelClube":"BRONZE","regiao":"SUL"}
                """.formatted(ITEM), "FORMA_PAGAMENTO_INVALIDA");
        recusa("""
                {"itens":[%s],"modalidadeEntrega":"EXPRESSA",
                 "nivelClube":"BRONZE","regiao":"SUL"}
                """.formatted(ITEM), "FORMA_PAGAMENTO_INVALIDA");
    }

    @Test
    @DisplayName("Pix e boleto so a vista; cartao de 1 a 12")
    void parcelamentoInvalido() throws Exception {
        recusa(pedidoPagamento("PIX", 2), "PARCELAMENTO_INVALIDO");
        recusa(pedidoPagamento("BOLETO", 3), "PARCELAMENTO_INVALIDO");
        recusa(pedidoPagamento("CARTAO", 13), "PARCELAMENTO_INVALIDO");
        recusa(pedidoPagamento("CARTAO", 0), "PARCELAMENTO_INVALIDO");
    }

    @Test
    @DisplayName("Boleto acima de R$ 1.000,00 no total do pedido")
    void formaPagamentoIndisponivel() throws Exception {
        recusa("""
                {"itens":[{"nome":"Jaqueta","precoUnitario":500.00,"quantidade":2,"pesoKg":1.0}],
                 "modalidadeEntrega":"RETIRADA_LOJA","formaPagamento":"BOLETO",
                 "nivelClube":"BRONZE","regiao":"SUL"}
                """, "FORMA_PAGAMENTO_INDISPONIVEL");
    }

    @Test
    @DisplayName("Corpo ilegivel conta como pedido invalido")
    void corpoIlegivel() throws Exception {
        recusa("{isso nao e json", "PEDIDO_INVALIDO");
        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().json("{\"erro\":\"PEDIDO_INVALIDO\"}", JsonCompareMode.STRICT));
    }

    private String pedidoCom(String item) {
        return """
                {"itens":[%s],"modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX",
                 "nivelClube":"BRONZE","regiao":"SUL"}
                """.formatted(item);
    }

    private String pedidoPagamento(String formaPagamento, int parcelas) {
        return """
                {"itens":[%s],"modalidadeEntrega":"EXPRESSA","formaPagamento":"%s",
                 "parcelas":%d,"nivelClube":"BRONZE","regiao":"SUL"}
                """.formatted(ITEM, formaPagamento, parcelas);
    }

    private void recusa(String requisicao, String codigoEsperado) throws Exception {
        mockMvc.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requisicao))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().json("{\"erro\":\"%s\"}".formatted(codigoEsperado), JsonCompareMode.STRICT));
    }
}
