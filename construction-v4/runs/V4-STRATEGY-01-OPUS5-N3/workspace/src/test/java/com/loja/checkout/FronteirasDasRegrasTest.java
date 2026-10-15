package com.loja.checkout;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Os limites exatos de cada regra e o contrato de recusa pela porta de
 * entrada: e no valor da fronteira que um operador trocado passa batido.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Fronteiras das regras e contrato de recusa")
class FronteirasDasRegrasTest {

    @Autowired
    private MockMvc cliente;

    @Test
    @DisplayName("motoboy leva pedido de exatamente 5 kg")
    void motoboyNoLimiteDoPeso() throws Exception {
        MvcResult resultado = enviar("""
                {"itens":[{"nome":"Caixa","precoUnitario":100.00,"quantidade":2,"pesoKg":2.50}],
                 "modalidadeEntrega":"MOTOBOY","formaPagamento":"PIX",
                 "nivelClube":"BRONZE","regiao":"SUDESTE"}
                """);

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(corpo(resultado)).contains("\"frete\":18.00", "\"totalFinal\":209.00");
    }

    @Test
    @DisplayName("MENOS50 vale com exatamente R$ 300,00 em produtos")
    void menos50NoLimiteDosProdutos() throws Exception {
        MvcResult resultado = enviar("""
                {"itens":[{"nome":"Calca","precoUnitario":150.00,"quantidade":2,"pesoKg":0.10}],
                 "modalidadeEntrega":"RETIRADA_LOJA","cupom":"MENOS50","formaPagamento":"PIX",
                 "nivelClube":"BRONZE","regiao":"SUDESTE"}
                """);

        assertThat(corpo(resultado)).contains("\"descontoCupom\":50.00", "\"totalFinal\":240.35");
    }

    @Test
    @DisplayName("boleto aceita total de exatamente R$ 1.000,00 e recusa um centavo acima")
    void boletoNoLimiteDoTotal() throws Exception {
        assertThat(corpo(enviar("""
                {"itens":[{"nome":"Jaqueta","precoUnitario":519.80,"quantidade":2,"pesoKg":0.50}],
                 "modalidadeEntrega":"RETIRADA_LOJA","cupom":"MENOS50","formaPagamento":"BOLETO",
                 "nivelClube":"BRONZE","regiao":"SUDESTE"}
                """))).contains("\"ajustePagamento\":3.49", "\"totalFinal\":1003.49");

        assertThat(corpo(enviar("""
                {"itens":[{"nome":"Jaqueta","precoUnitario":519.85,"quantidade":2,"pesoKg":0.50}],
                 "modalidadeEntrega":"RETIRADA_LOJA","cupom":"MENOS50","formaPagamento":"BOLETO",
                 "nivelClube":"BRONZE","regiao":"SUDESTE"}
                """))).isEqualTo("{\"erro\":\"FORMA_PAGAMENTO_INDISPONIVEL\"}");
    }

    @Test
    @DisplayName("OURO com exatamente R$ 500,00 em produtos nao leva brinde")
    void brindeSoAcimaDeQuinhentos() throws Exception {
        assertThat(corpo(enviar("""
                {"itens":[{"nome":"Bolsa","precoUnitario":250.00,"quantidade":2,"pesoKg":0.40}],
                 "modalidadeEntrega":"RETIRADA_LOJA","formaPagamento":"PIX",
                 "nivelClube":"OURO","regiao":"SUDESTE"}
                """))).contains("\"brinde\":false", "\"creditoProximaCompra\":25.00");
    }

    @Test
    @DisplayName("LEVE3PAGUE2 sem nenhum item com 3 unidades: vale, com desconto zero")
    void leve3Pague2SemTrioNaoRecusaOPedido() throws Exception {
        assertThat(corpo(enviar("""
                {"itens":[{"nome":"Camiseta","precoUnitario":79.90,"quantidade":2,"pesoKg":0.30}],
                 "modalidadeEntrega":"RETIRADA_LOJA","cupom":"LEVE3PAGUE2","formaPagamento":"PIX",
                 "nivelClube":"BRONZE","regiao":"SUDESTE"}
                """))).contains("\"descontoCupom\":0.00", "\"totalFinal\":153.33");
    }

    @Test
    @DisplayName("FRETEGRATIS para OURO: o frete ja era zero, o desconto tambem")
    void freteGratisParaQuemJaNaoPagaFrete() throws Exception {
        assertThat(corpo(enviar("""
                {"itens":[{"nome":"Tenis","precoUnitario":249.90,"quantidade":1,"pesoKg":1.20}],
                 "modalidadeEntrega":"EXPRESSA","cupom":"FRETEGRATIS","formaPagamento":"PIX",
                 "nivelClube":"OURO","regiao":"SUDESTE"}
                """))).contains("\"frete\":0.00", "\"descontoCupom\":0.00");
    }

    @Test
    @DisplayName("pedido recusado devolve so o codigo do problema, com status 400")
    void contratoDaRecusa() throws Exception {
        MvcResult resultado = enviar("""
                {"itens":[],"modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX",
                 "nivelClube":"BRONZE","regiao":"SUDESTE"}
                """);

        assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
        assertThat(corpo(resultado)).isEqualTo("{\"erro\":\"PEDIDO_INVALIDO\"}");
    }

    @Test
    @DisplayName("corpo ilegivel ou ausente tambem e pedido invalido")
    void corpoIlegivelOuAusente() throws Exception {
        assertThat(corpo(enviar("isso nao e json"))).isEqualTo("{\"erro\":\"PEDIDO_INVALIDO\"}");
        assertThat(corpo(enviar(null))).isEqualTo("{\"erro\":\"PEDIDO_INVALIDO\"}");
    }

    private MvcResult enviar(String corpo) throws Exception {
        var requisicao = MockMvcRequestBuilders.post("/checkout/resumo")
                .contentType(MediaType.APPLICATION_JSON);
        return cliente.perform(corpo == null ? requisicao : requisicao.content(corpo)).andReturn();
    }

    private String corpo(MvcResult resultado) throws Exception {
        return resultado.getResponse().getContentAsString();
    }
}
