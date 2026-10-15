package com.loja.checkout;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Pedidos recusados: o servico devolve so o codigo do problema. */
@SpringBootTest
@AutoConfigureMockMvc
class RecusaDoPedidoTest {

    private static final String ITEM_OK =
            "{\"nome\":\"Camiseta\",\"precoUnitario\":79.90,\"quantidade\":2,\"pesoKg\":0.30}";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("carrinho vazio")
    void carrinhoVazio() throws Exception {
        esperaErro("""
                {"itens":[],"modalidadeEntrega":"ECONOMICA","formaPagamento":"PIX",
                 "nivelClube":"BRONZE","regiao":"SUDESTE"}
                """, "PEDIDO_INVALIDO");
    }

    @Test
    @DisplayName("sem a lista de itens")
    void semItens() throws Exception {
        esperaErro("""
                {"modalidadeEntrega":"ECONOMICA","formaPagamento":"PIX",
                 "nivelClube":"BRONZE","regiao":"SUDESTE"}
                """, "PEDIDO_INVALIDO");
    }

    @Test
    @DisplayName("item com preco zero, quantidade negativa ou peso ausente")
    void itemInvalido() throws Exception {
        esperaErro(pedidoCom("{\"nome\":\"X\",\"precoUnitario\":0,\"quantidade\":1,\"pesoKg\":0.30}"),
                "PEDIDO_INVALIDO");
        esperaErro(pedidoCom("{\"nome\":\"X\",\"precoUnitario\":79.90,\"quantidade\":-1,\"pesoKg\":0.30}"),
                "PEDIDO_INVALIDO");
        esperaErro(pedidoCom("{\"nome\":\"X\",\"precoUnitario\":79.90,\"quantidade\":1}"),
                "PEDIDO_INVALIDO");
        esperaErro(pedidoCom("{\"nome\":\"X\",\"precoUnitario\":79.90,\"quantidade\":1,\"pesoKg\":-0.30}"),
                "PEDIDO_INVALIDO");
    }

    @Test
    @DisplayName("nivel do clube inexistente ou nao informado")
    void nivelClubeInvalido() throws Exception {
        esperaErro("""
                {"itens":[%s],"modalidadeEntrega":"ECONOMICA","formaPagamento":"PIX",
                 "nivelClube":"DIAMANTE","regiao":"SUDESTE"}
                """.formatted(ITEM_OK), "NIVEL_CLUBE_INVALIDO");
        esperaErro("""
                {"itens":[%s],"modalidadeEntrega":"ECONOMICA","formaPagamento":"PIX",
                 "regiao":"SUDESTE"}
                """.formatted(ITEM_OK), "NIVEL_CLUBE_INVALIDO");
    }

    @Test
    @DisplayName("regiao inexistente ou nao informada")
    void regiaoInvalida() throws Exception {
        esperaErro("""
                {"itens":[%s],"modalidadeEntrega":"ECONOMICA","formaPagamento":"PIX",
                 "nivelClube":"BRONZE","regiao":"EXTERIOR"}
                """.formatted(ITEM_OK), "REGIAO_INVALIDA");
        esperaErro("""
                {"itens":[%s],"modalidadeEntrega":"ECONOMICA","formaPagamento":"PIX",
                 "nivelClube":"BRONZE"}
                """.formatted(ITEM_OK), "REGIAO_INVALIDA");
    }

    @Test
    @DisplayName("modalidade de entrega inexistente, nao informada ou em minusculas")
    void modalidadeInvalida() throws Exception {
        esperaErro("""
                {"itens":[%s],"modalidadeEntrega":"DRONE","formaPagamento":"PIX",
                 "nivelClube":"BRONZE","regiao":"SUDESTE"}
                """.formatted(ITEM_OK), "MODALIDADE_INVALIDA");
        esperaErro("""
                {"itens":[%s],"formaPagamento":"PIX",
                 "nivelClube":"BRONZE","regiao":"SUDESTE"}
                """.formatted(ITEM_OK), "MODALIDADE_INVALIDA");
        esperaErro("""
                {"itens":[%s],"modalidadeEntrega":"expressa","formaPagamento":"PIX",
                 "nivelClube":"BRONZE","regiao":"SUDESTE"}
                """.formatted(ITEM_OK), "MODALIDADE_INVALIDA");
    }

    @Test
    @DisplayName("motoboy acima de 5 kg")
    void motoboyPesado() throws Exception {
        esperaErro("""
                {"itens":[{"nome":"Mala","precoUnitario":199.90,"quantidade":3,"pesoKg":2.00}],
                 "modalidadeEntrega":"MOTOBOY","formaPagamento":"PIX",
                 "nivelClube":"BRONZE","regiao":"SUDESTE"}
                """, "MODALIDADE_INDISPONIVEL");
    }

    @Test
    @DisplayName("motoboy com exatamente 5 kg e aceito")
    void motoboyNoLimite() throws Exception {
        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content("""
                        {"itens":[{"nome":"Mala","precoUnitario":100.00,"quantidade":2,"pesoKg":2.50}],
                         "modalidadeEntrega":"MOTOBOY","formaPagamento":"PIX",
                         "nivelClube":"BRONZE","regiao":"SUDESTE"}
                        """))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"frete\":18.00,\"prazoEntregaDias\":0}"));
    }

    @Test
    @DisplayName("cupom que nao existe")
    void cupomInvalido() throws Exception {
        esperaErro("""
                {"itens":[%s],"modalidadeEntrega":"ECONOMICA","cupom":"PROMO2026",
                 "formaPagamento":"PIX","nivelClube":"BRONZE","regiao":"SUDESTE"}
                """.formatted(ITEM_OK), "CUPOM_INVALIDO");
    }

    @Test
    @DisplayName("MENOS50 abaixo de R$ 300,00 em produtos")
    void cupomNaoAplicavel() throws Exception {
        esperaErro("""
                {"itens":[%s],"modalidadeEntrega":"ECONOMICA","cupom":"MENOS50",
                 "formaPagamento":"PIX","nivelClube":"BRONZE","regiao":"SUDESTE"}
                """.formatted(ITEM_OK), "CUPOM_NAO_APLICAVEL");
    }

    @Test
    @DisplayName("forma de pagamento inexistente ou nao informada")
    void formaPagamentoInvalida() throws Exception {
        esperaErro("""
                {"itens":[%s],"modalidadeEntrega":"ECONOMICA","formaPagamento":"DINHEIRO",
                 "nivelClube":"BRONZE","regiao":"SUDESTE"}
                """.formatted(ITEM_OK), "FORMA_PAGAMENTO_INVALIDA");
        esperaErro("""
                {"itens":[%s],"modalidadeEntrega":"ECONOMICA",
                 "nivelClube":"BRONZE","regiao":"SUDESTE"}
                """.formatted(ITEM_OK), "FORMA_PAGAMENTO_INVALIDA");
    }

    @Test
    @DisplayName("parcelamento nao permitido: Pix e boleto so 1x, cartao de 1x a 12x")
    void parcelamentoInvalido() throws Exception {
        esperaErro("""
                {"itens":[%s],"modalidadeEntrega":"ECONOMICA","formaPagamento":"PIX","parcelas":2,
                 "nivelClube":"BRONZE","regiao":"SUDESTE"}
                """.formatted(ITEM_OK), "PARCELAMENTO_INVALIDO");
        esperaErro("""
                {"itens":[%s],"modalidadeEntrega":"ECONOMICA","formaPagamento":"BOLETO","parcelas":3,
                 "nivelClube":"BRONZE","regiao":"SUDESTE"}
                """.formatted(ITEM_OK), "PARCELAMENTO_INVALIDO");
        esperaErro("""
                {"itens":[%s],"modalidadeEntrega":"ECONOMICA","formaPagamento":"CARTAO","parcelas":13,
                 "nivelClube":"BRONZE","regiao":"SUDESTE"}
                """.formatted(ITEM_OK), "PARCELAMENTO_INVALIDO");
        esperaErro("""
                {"itens":[%s],"modalidadeEntrega":"ECONOMICA","formaPagamento":"CARTAO","parcelas":0,
                 "nivelClube":"BRONZE","regiao":"SUDESTE"}
                """.formatted(ITEM_OK), "PARCELAMENTO_INVALIDO");
    }

    @Test
    @DisplayName("boleto com total do pedido acima de R$ 1.000,00")
    void boletoAcimaDoLimite() throws Exception {
        esperaErro("""
                {"itens":[{"nome":"Tenis","precoUnitario":249.90,"quantidade":5,"pesoKg":1.20}],
                 "modalidadeEntrega":"RETIRADA_LOJA","formaPagamento":"BOLETO",
                 "nivelClube":"BRONZE","regiao":"SUDESTE"}
                """, "FORMA_PAGAMENTO_INDISPONIVEL");
    }

    @Test
    @DisplayName("com mais de um problema, vale o primeiro da ordem de conferencia")
    void ordemDeConferencia() throws Exception {
        // carrinho vazio + clube invalido + regiao invalida
        esperaErro("""
                {"itens":[],"modalidadeEntrega":"DRONE","formaPagamento":"DINHEIRO",
                 "nivelClube":"DIAMANTE","regiao":"EXTERIOR"}
                """, "PEDIDO_INVALIDO");
        // clube invalido + modalidade invalida
        esperaErro("""
                {"itens":[%s],"modalidadeEntrega":"DRONE","formaPagamento":"DINHEIRO",
                 "nivelClube":"DIAMANTE","regiao":"SUDESTE"}
                """.formatted(ITEM_OK), "NIVEL_CLUBE_INVALIDO");
        // motoboy pesado + cupom invalido
        esperaErro("""
                {"itens":[{"nome":"Mala","precoUnitario":199.90,"quantidade":3,"pesoKg":2.00}],
                 "modalidadeEntrega":"MOTOBOY","cupom":"PROMO2026","formaPagamento":"DINHEIRO",
                 "nivelClube":"BRONZE","regiao":"SUDESTE"}
                """, "MODALIDADE_INDISPONIVEL");
        // cupom invalido + forma de pagamento invalida
        esperaErro("""
                {"itens":[%s],"modalidadeEntrega":"ECONOMICA","cupom":"PROMO2026",
                 "formaPagamento":"DINHEIRO","nivelClube":"BRONZE","regiao":"SUDESTE"}
                """.formatted(ITEM_OK), "CUPOM_INVALIDO");
        // parcelamento invalido vem antes do boleto acima do limite
        esperaErro("""
                {"itens":[{"nome":"Tenis","precoUnitario":249.90,"quantidade":5,"pesoKg":1.20}],
                 "modalidadeEntrega":"RETIRADA_LOJA","formaPagamento":"BOLETO","parcelas":2,
                 "nivelClube":"BRONZE","regiao":"SUDESTE"}
                """, "PARCELAMENTO_INVALIDO");
    }

    @Test
    @DisplayName("corpo mal formado")
    void corpoMalFormado() throws Exception {
        esperaErro("{\"itens\":", "PEDIDO_INVALIDO");
    }

    private String pedidoCom(String item) {
        return """
                {"itens":[%s],"modalidadeEntrega":"ECONOMICA","formaPagamento":"PIX",
                 "nivelClube":"BRONZE","regiao":"SUDESTE"}
                """.formatted(item);
    }

    private void esperaErro(String pedido, String codigo) throws Exception {
        mockMvc.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pedido))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"erro\":\"%s\"}".formatted(codigo)));
    }
}
