package br.com.loja.checkout;

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
class RecusaDePedidoTest {

    @Autowired
    private MockMvc mockMvc;

    /** Pedido valido; cada teste troca uma peca para provocar a recusa. */
    private String pedido(String camposTrocados) {
        return """
                {"itens": [{"nome": "Camiseta", "precoUnitario": 79.90, "quantidade": 2, "pesoKg": 0.30}],
                 "modalidadeEntrega": "EXPRESSA",
                 "formaPagamento": "PIX", "parcelas": 1,
                 "nivelClube": "BRONZE", "regiao": "SUDESTE"
                 %s}
                """.formatted(camposTrocados.isEmpty() ? "" : ", " + camposTrocados);
    }

    private void esperaErro(String corpo, String codigo) throws Exception {
        mockMvc.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value(codigo))
                .andExpect(jsonPath("$.totalFinal").doesNotExist());
    }

    @Test
    @DisplayName("Carrinho vazio")
    void carrinhoVazio() throws Exception {
        esperaErro(pedido("\"itens\": []"), "PEDIDO_INVALIDO");
    }

    @Test
    @DisplayName("Item com quantidade zero")
    void quantidadeZero() throws Exception {
        esperaErro(pedido("""
                "itens": [{"nome": "Meia", "precoUnitario": 19.90, "quantidade": 0, "pesoKg": 0.10}]"""),
                "PEDIDO_INVALIDO");
    }

    @Test
    @DisplayName("Item sem peso")
    void itemSemPeso() throws Exception {
        esperaErro(pedido("""
                "itens": [{"nome": "Meia", "precoUnitario": 19.90, "quantidade": 1}]"""),
                "PEDIDO_INVALIDO");
    }

    @Test
    @DisplayName("Item com preco negativo")
    void precoNegativo() throws Exception {
        esperaErro(pedido("""
                "itens": [{"nome": "Meia", "precoUnitario": -19.90, "quantidade": 1, "pesoKg": 0.10}]"""),
                "PEDIDO_INVALIDO");
    }

    @Test
    @DisplayName("Nivel de clube que nao existe")
    void nivelClubeInvalido() throws Exception {
        esperaErro(pedido("\"nivelClube\": \"DIAMANTE\""), "NIVEL_CLUBE_INVALIDO");
    }

    @Test
    @DisplayName("Regiao que nao existe")
    void regiaoInvalida() throws Exception {
        esperaErro(pedido("\"regiao\": \"SUDESTEE\""), "REGIAO_INVALIDA");
    }

    @Test
    @DisplayName("Modalidade de entrega que nao existe")
    void modalidadeInvalida() throws Exception {
        esperaErro(pedido("\"modalidadeEntrega\": \"DRONE\""), "MODALIDADE_INVALIDA");
    }

    @Test
    @DisplayName("Motoboy acima de 5 kg")
    void motoboyAcimaDoLimite() throws Exception {
        esperaErro(pedido("""
                "modalidadeEntrega": "MOTOBOY",
                 "itens": [{"nome": "Mala", "precoUnitario": 199.90, "quantidade": 2, "pesoKg": 3.00}]"""),
                "MODALIDADE_INDISPONIVEL");
    }

    @Test
    @DisplayName("Motoboy em exatamente 5 kg atende")
    void motoboyNoLimiteAtende() throws Exception {
        mockMvc.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pedido("""
                                "modalidadeEntrega": "MOTOBOY",
                                 "itens": [{"nome": "Mala", "precoUnitario": 100.00, "quantidade": 1, "pesoKg": 5.00}]""")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.frete").value(18.00));
    }

    @Test
    @DisplayName("Cupom que nao existe")
    void cupomInvalido() throws Exception {
        esperaErro(pedido("\"cupom\": \"PROMOCAOX\""), "CUPOM_INVALIDO");
    }

    @Test
    @DisplayName("Cupom em minusculas nao vale")
    void cupomEmMinusculas() throws Exception {
        esperaErro(pedido("\"cupom\": \"bemvindo10\""), "CUPOM_INVALIDO");
    }

    @Test
    @DisplayName("MENOS50 abaixo de 300 em produtos")
    void menos50NaoAplicavel() throws Exception {
        esperaErro(pedido("\"cupom\": \"MENOS50\""), "CUPOM_NAO_APLICAVEL");
    }

    @Test
    @DisplayName("Forma de pagamento que nao existe")
    void formaPagamentoInvalida() throws Exception {
        esperaErro(pedido("\"formaPagamento\": \"CRIPTO\""), "FORMA_PAGAMENTO_INVALIDA");
    }

    @Test
    @DisplayName("Pix parcelado")
    void pixParceladoNaoVale() throws Exception {
        esperaErro(pedido("\"parcelas\": 2"), "PARCELAMENTO_INVALIDO");
    }

    @Test
    @DisplayName("Cartao em 13x")
    void cartaoAcimaDoMaximo() throws Exception {
        esperaErro(pedido("\"formaPagamento\": \"CARTAO\", \"parcelas\": 13"),
                "PARCELAMENTO_INVALIDO");
    }

    @Test
    @DisplayName("Boleto acima de 1000 no total do pedido")
    void boletoAcimaDoLimite() throws Exception {
        esperaErro(pedido("""
                "formaPagamento": "BOLETO",
                 "itens": [{"nome": "Sofa", "precoUnitario": 1200.00, "quantidade": 1, "pesoKg": 2.00}]"""),
                "FORMA_PAGAMENTO_INDISPONIVEL");
    }

    @Test
    @DisplayName("A ordem das conferencias: vale a primeira da lista")
    void ordemDasConferencias() throws Exception {
        esperaErro("""
                {"itens": [], "modalidadeEntrega": "DRONE", "cupom": "PROMOCAOX",
                 "formaPagamento": "CRIPTO", "nivelClube": "DIAMANTE", "regiao": "LESTE"}
                """, "PEDIDO_INVALIDO");
        esperaErro("""
                {"itens": [{"nome": "Meia", "precoUnitario": 19.90, "quantidade": 1, "pesoKg": 0.10}],
                 "modalidadeEntrega": "DRONE", "cupom": "PROMOCAOX",
                 "formaPagamento": "CRIPTO", "nivelClube": "DIAMANTE", "regiao": "LESTE"}
                """, "NIVEL_CLUBE_INVALIDO");
        esperaErro("""
                {"itens": [{"nome": "Meia", "precoUnitario": 19.90, "quantidade": 1, "pesoKg": 0.10}],
                 "modalidadeEntrega": "DRONE", "cupom": "PROMOCAOX",
                 "formaPagamento": "CRIPTO", "nivelClube": "BRONZE", "regiao": "LESTE"}
                """, "REGIAO_INVALIDA");
        esperaErro("""
                {"itens": [{"nome": "Meia", "precoUnitario": 19.90, "quantidade": 1, "pesoKg": 0.10}],
                 "modalidadeEntrega": "DRONE", "cupom": "PROMOCAOX",
                 "formaPagamento": "CRIPTO", "nivelClube": "BRONZE", "regiao": "SUL"}
                """, "MODALIDADE_INVALIDA");
        esperaErro("""
                {"itens": [{"nome": "Meia", "precoUnitario": 19.90, "quantidade": 1, "pesoKg": 0.10}],
                 "modalidadeEntrega": "EXPRESSA", "cupom": "PROMOCAOX",
                 "formaPagamento": "CRIPTO", "nivelClube": "BRONZE", "regiao": "SUL"}
                """, "CUPOM_INVALIDO");
        esperaErro("""
                {"itens": [{"nome": "Meia", "precoUnitario": 19.90, "quantidade": 1, "pesoKg": 0.10}],
                 "modalidadeEntrega": "EXPRESSA", "cupom": "MENOS50",
                 "formaPagamento": "CRIPTO", "nivelClube": "BRONZE", "regiao": "SUL"}
                """, "CUPOM_NAO_APLICAVEL");
    }

    @Test
    @DisplayName("Campos ausentes e corpo ilegivel")
    void pedidoSemCampos() throws Exception {
        esperaErro("{}", "PEDIDO_INVALIDO");
        esperaErro("nao e json", "PEDIDO_INVALIDO");
    }

    @Test
    @DisplayName("Forma de pagamento ausente")
    void formaPagamentoAusente() throws Exception {
        esperaErro("""
                {"itens": [{"nome": "Meia", "precoUnitario": 19.90, "quantidade": 1, "pesoKg": 0.10}],
                 "modalidadeEntrega": "EXPRESSA", "nivelClube": "BRONZE", "regiao": "SUL"}
                """, "FORMA_PAGAMENTO_INVALIDA");
    }
}
