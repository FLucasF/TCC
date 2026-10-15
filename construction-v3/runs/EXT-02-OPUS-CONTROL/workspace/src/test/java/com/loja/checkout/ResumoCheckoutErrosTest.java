package com.loja.checkout;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ResumoCheckoutErrosTest {

    @Autowired
    private MockMvc mockMvc;

    private void esperaErro(String corpo, String codigo) throws Exception {
        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value(codigo));
    }

    @Test
    @DisplayName("Carrinho vazio ou sem a lista de itens")
    void carrinhoVazio() throws Exception {
        esperaErro("""
                { "itens": [], "modalidadeEntrega": "EXPRESSA", "formaPagamento": "PIX",
                  "nivelClube": "BRONZE", "regiao": "SUDESTE" }
                """, "PEDIDO_INVALIDO");
        esperaErro("""
                { "modalidadeEntrega": "EXPRESSA", "formaPagamento": "PIX",
                  "nivelClube": "BRONZE", "regiao": "SUDESTE" }
                """, "PEDIDO_INVALIDO");
    }

    @Test
    @DisplayName("Item com preco, quantidade ou peso invalido")
    void itemInvalido() throws Exception {
        String modelo = """
                { "itens": [{ "nome": "Meia", %s }], "modalidadeEntrega": "RETIRADA_LOJA",
                  "formaPagamento": "PIX", "nivelClube": "BRONZE", "regiao": "SUDESTE" }
                """;
        esperaErro(modelo.formatted("\"precoUnitario\": 0, \"quantidade\": 1, \"pesoKg\": 0.10"), "PEDIDO_INVALIDO");
        esperaErro(modelo.formatted("\"precoUnitario\": -5, \"quantidade\": 1, \"pesoKg\": 0.10"), "PEDIDO_INVALIDO");
        esperaErro(modelo.formatted("\"precoUnitario\": 19.90, \"quantidade\": 0, \"pesoKg\": 0.10"), "PEDIDO_INVALIDO");
        esperaErro(modelo.formatted("\"precoUnitario\": 19.90, \"quantidade\": -1, \"pesoKg\": 0.10"), "PEDIDO_INVALIDO");
        esperaErro(modelo.formatted("\"precoUnitario\": 19.90, \"quantidade\": 1, \"pesoKg\": 0"), "PEDIDO_INVALIDO");
        esperaErro(modelo.formatted("\"precoUnitario\": 19.90, \"quantidade\": 1"), "PEDIDO_INVALIDO");
        esperaErro(modelo.formatted("\"quantidade\": 1, \"pesoKg\": 0.10"), "PEDIDO_INVALIDO");
    }

    @Test
    @DisplayName("Nivel do clube inexistente ou ausente")
    void nivelClubeInvalido() throws Exception {
        esperaErro("""
                { "itens": [{ "nome": "Meia", "precoUnitario": 19.90, "quantidade": 1, "pesoKg": 0.10 }],
                  "modalidadeEntrega": "RETIRADA_LOJA", "formaPagamento": "PIX",
                  "nivelClube": "DIAMANTE", "regiao": "SUDESTE" }
                """, "NIVEL_CLUBE_INVALIDO");
        esperaErro("""
                { "itens": [{ "nome": "Meia", "precoUnitario": 19.90, "quantidade": 1, "pesoKg": 0.10 }],
                  "modalidadeEntrega": "RETIRADA_LOJA", "formaPagamento": "PIX", "regiao": "SUDESTE" }
                """, "NIVEL_CLUBE_INVALIDO");
    }

    @Test
    @DisplayName("Regiao inexistente ou ausente")
    void regiaoInvalida() throws Exception {
        esperaErro("""
                { "itens": [{ "nome": "Meia", "precoUnitario": 19.90, "quantidade": 1, "pesoKg": 0.10 }],
                  "modalidadeEntrega": "RETIRADA_LOJA", "formaPagamento": "PIX",
                  "nivelClube": "BRONZE", "regiao": "EUROPA" }
                """, "REGIAO_INVALIDA");
        esperaErro("""
                { "itens": [{ "nome": "Meia", "precoUnitario": 19.90, "quantidade": 1, "pesoKg": 0.10 }],
                  "modalidadeEntrega": "RETIRADA_LOJA", "formaPagamento": "PIX", "nivelClube": "BRONZE" }
                """, "REGIAO_INVALIDA");
    }

    @Test
    @DisplayName("Modalidade de entrega inexistente ou ausente")
    void modalidadeInvalida() throws Exception {
        esperaErro("""
                { "itens": [{ "nome": "Meia", "precoUnitario": 19.90, "quantidade": 1, "pesoKg": 0.10 }],
                  "modalidadeEntrega": "DRONE", "formaPagamento": "PIX",
                  "nivelClube": "BRONZE", "regiao": "SUDESTE" }
                """, "MODALIDADE_INVALIDA");
        esperaErro("""
                { "itens": [{ "nome": "Meia", "precoUnitario": 19.90, "quantidade": 1, "pesoKg": 0.10 }],
                  "formaPagamento": "PIX", "nivelClube": "BRONZE", "regiao": "SUDESTE" }
                """, "MODALIDADE_INVALIDA");
    }

    @Test
    @DisplayName("Motoboy nao leva acima de 5 kg")
    void motoboyAcimaDoPeso() throws Exception {
        esperaErro("""
                { "itens": [{ "nome": "Tenis", "precoUnitario": 249.90, "quantidade": 5, "pesoKg": 1.20 }],
                  "modalidadeEntrega": "MOTOBOY", "formaPagamento": "PIX",
                  "nivelClube": "BRONZE", "regiao": "SUDESTE" }
                """, "MODALIDADE_INDISPONIVEL");
    }

    @Test
    @DisplayName("Motoboy leva exatamente 5 kg")
    void motoboyNoLimiteDoPeso() throws Exception {
        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content("""
                { "itens": [{ "nome": "Tenis", "precoUnitario": 100.00, "quantidade": 4, "pesoKg": 1.25 }],
                  "modalidadeEntrega": "MOTOBOY", "formaPagamento": "PIX",
                  "nivelClube": "BRONZE", "regiao": "SUDESTE" }
                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.frete").value(18.00));
    }

    @Test
    @DisplayName("Cupom inexistente")
    void cupomInvalido() throws Exception {
        esperaErro("""
                { "itens": [{ "nome": "Meia", "precoUnitario": 19.90, "quantidade": 1, "pesoKg": 0.10 }],
                  "modalidadeEntrega": "RETIRADA_LOJA", "cupom": "PROMO999", "formaPagamento": "PIX",
                  "nivelClube": "BRONZE", "regiao": "SUDESTE" }
                """, "CUPOM_INVALIDO");
    }

    @Test
    @DisplayName("Cupom em minusculas nao vale")
    void cupomEmMinusculas() throws Exception {
        esperaErro("""
                { "itens": [{ "nome": "Meia", "precoUnitario": 19.90, "quantidade": 1, "pesoKg": 0.10 }],
                  "modalidadeEntrega": "RETIRADA_LOJA", "cupom": "bemvindo10", "formaPagamento": "PIX",
                  "nivelClube": "BRONZE", "regiao": "SUDESTE" }
                """, "CUPOM_INVALIDO");
    }

    @Test
    @DisplayName("MENOS50 abaixo de R$ 300,00 em produtos")
    void cupomNaoAplicavel() throws Exception {
        esperaErro("""
                { "itens": [{ "nome": "Camiseta", "precoUnitario": 79.90, "quantidade": 3, "pesoKg": 0.30 }],
                  "modalidadeEntrega": "RETIRADA_LOJA", "cupom": "MENOS50", "formaPagamento": "PIX",
                  "nivelClube": "BRONZE", "regiao": "SUDESTE" }
                """, "CUPOM_NAO_APLICAVEL");
    }

    @Test
    @DisplayName("Forma de pagamento inexistente ou ausente")
    void formaPagamentoInvalida() throws Exception {
        esperaErro("""
                { "itens": [{ "nome": "Meia", "precoUnitario": 19.90, "quantidade": 1, "pesoKg": 0.10 }],
                  "modalidadeEntrega": "RETIRADA_LOJA", "formaPagamento": "DINHEIRO",
                  "nivelClube": "BRONZE", "regiao": "SUDESTE" }
                """, "FORMA_PAGAMENTO_INVALIDA");
        esperaErro("""
                { "itens": [{ "nome": "Meia", "precoUnitario": 19.90, "quantidade": 1, "pesoKg": 0.10 }],
                  "modalidadeEntrega": "RETIRADA_LOJA", "nivelClube": "BRONZE", "regiao": "SUDESTE" }
                """, "FORMA_PAGAMENTO_INVALIDA");
    }

    @Test
    @DisplayName("Pix e boleto sao sempre a vista; cartao vai de 1x a 12x")
    void parcelamentoInvalido() throws Exception {
        String modelo = """
                { "itens": [{ "nome": "Meia", "precoUnitario": 19.90, "quantidade": 1, "pesoKg": 0.10 }],
                  "modalidadeEntrega": "RETIRADA_LOJA", "formaPagamento": "%s", "parcelas": %d,
                  "nivelClube": "BRONZE", "regiao": "SUDESTE" }
                """;
        esperaErro(modelo.formatted("PIX", 2), "PARCELAMENTO_INVALIDO");
        esperaErro(modelo.formatted("BOLETO", 3), "PARCELAMENTO_INVALIDO");
        esperaErro(modelo.formatted("CARTAO", 13), "PARCELAMENTO_INVALIDO");
        esperaErro(modelo.formatted("CARTAO", 0), "PARCELAMENTO_INVALIDO");
        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON)
                .content(modelo.formatted("CARTAO", 12)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parcelas").value(12));
    }

    @Test
    @DisplayName("Boleto nao vale acima de R$ 1.000,00 de total do pedido")
    void boletoAcimaDoLimite() throws Exception {
        esperaErro("""
                { "itens": [{ "nome": "Sofa", "precoUnitario": 600.00, "quantidade": 2, "pesoKg": 1.00 }],
                  "modalidadeEntrega": "RETIRADA_LOJA", "formaPagamento": "BOLETO",
                  "nivelClube": "BRONZE", "regiao": "SUDESTE" }
                """, "FORMA_PAGAMENTO_INDISPONIVEL");
    }

    @Test
    @DisplayName("Boleto vale em exatamente R$ 1.000,00")
    void boletoNoLimite() throws Exception {
        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content("""
                { "itens": [{ "nome": "Mesa", "precoUnitario": 500.00, "quantidade": 2, "pesoKg": 1.00 }],
                  "modalidadeEntrega": "RETIRADA_LOJA", "formaPagamento": "BOLETO",
                  "nivelClube": "BRONZE", "regiao": "SUDESTE" }
                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ajustePagamento").value(3.49));
    }

    @Test
    @DisplayName("Com varios problemas, vale a ordem combinada")
    void ordemDeVerificacao() throws Exception {
        // carrinho vazio ganha de todo o resto
        esperaErro("""
                { "itens": [], "modalidadeEntrega": "DRONE", "cupom": "PROMO999",
                  "formaPagamento": "DINHEIRO", "nivelClube": "DIAMANTE", "regiao": "EUROPA" }
                """, "PEDIDO_INVALIDO");
        // nivel do clube ganha da regiao e da modalidade
        esperaErro("""
                { "itens": [{ "nome": "Meia", "precoUnitario": 19.90, "quantidade": 1, "pesoKg": 0.10 }],
                  "modalidadeEntrega": "DRONE", "formaPagamento": "DINHEIRO",
                  "nivelClube": "DIAMANTE", "regiao": "EUROPA" }
                """, "NIVEL_CLUBE_INVALIDO");
        // modalidade indisponivel ganha do cupom invalido
        esperaErro("""
                { "itens": [{ "nome": "Tenis", "precoUnitario": 249.90, "quantidade": 5, "pesoKg": 1.20 }],
                  "modalidadeEntrega": "MOTOBOY", "cupom": "PROMO999", "formaPagamento": "DINHEIRO",
                  "nivelClube": "BRONZE", "regiao": "SUDESTE" }
                """, "MODALIDADE_INDISPONIVEL");
        // cupom nao aplicavel ganha da forma de pagamento inexistente
        esperaErro("""
                { "itens": [{ "nome": "Meia", "precoUnitario": 19.90, "quantidade": 1, "pesoKg": 0.10 }],
                  "modalidadeEntrega": "RETIRADA_LOJA", "cupom": "MENOS50", "formaPagamento": "DINHEIRO",
                  "nivelClube": "BRONZE", "regiao": "SUDESTE" }
                """, "CUPOM_NAO_APLICAVEL");
        // parcelamento ganha da indisponibilidade do boleto
        esperaErro("""
                { "itens": [{ "nome": "Sofa", "precoUnitario": 600.00, "quantidade": 2, "pesoKg": 1.00 }],
                  "modalidadeEntrega": "RETIRADA_LOJA", "formaPagamento": "BOLETO", "parcelas": 2,
                  "nivelClube": "BRONZE", "regiao": "SUDESTE" }
                """, "PARCELAMENTO_INVALIDO");
    }

    @Test
    @DisplayName("Corpo fora do formato combinado")
    void corpoInvalido() throws Exception {
        esperaErro("{ \"itens\": \"nao e uma lista\" }", "PEDIDO_INVALIDO");
    }
}
