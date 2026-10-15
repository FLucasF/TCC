package com.loja;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.loja.dominio.ErroNegocio;
import com.loja.dominio.Transportadora;

@SpringBootTest
class FluxoTest {

    @TestConfiguration
    static class Config {
        @Bean
        @Primary
        Transportadora transportadoraFalsa() {
            return (cep, peso, servico) -> {
                if (cep.equals("99999999")) {
                    return Optional.empty();
                }
                if (cep.equals("88888888")) {
                    throw new ErroNegocio("TRANSPORTADORA_INDISPONIVEL", 503);
                }
                return Optional.of(new Transportadora.Cotacao(new BigDecimal("33.105"), 2));
            };
        }
    }

    @Autowired
    WebApplicationContext contexto;

    MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).build();
    }

    static String compra(String cpf, String itens, String entrega, String cupom, String pagamento, int parcelas,
                         String nivel, String regiao, String cep) {
        return """
                {"cliente":{"cpf":"%s","email":"a@b.com"},"itens":%s,"cep":"%s","regiao":"%s",
                 "modalidadeEntrega":"%s",%s"formaPagamento":"%s","parcelas":%d,"nivelClube":"%s"}"""
                .formatted(cpf, itens, cep, regiao, entrega, cupom == null ? "" : "\"cupom\":\"" + cupom + "\",",
                        pagamento, parcelas, nivel);
    }

    ResultActions chamar(String caminho, String corpo) throws Exception {
        return mvc.perform(post(caminho).contentType(MediaType.APPLICATION_JSON).content(corpo));
    }

    @Test
    void resumoPixSemCupom() throws Exception {
        chamar("/checkout/resumo", compra("1", "[{\"sku\":\"CAM-001\",\"quantidade\":2}]", "RETIRADA_LOJA", null,
                "PIX", 1, "BRONZE", "SUDESTE", "01310100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotalProdutos").value(159.80))
                .andExpect(jsonPath("$.seguro").value(1.60))
                .andExpect(jsonPath("$.ajustePagamento").value(-8.07))
                .andExpect(jsonPath("$.totalFinal").value(153.33))
                .andExpect(jsonPath("$.prazoEntregaDias").value(1));
    }

    @Test
    void transportadoraArredondaEAcrescentaPrazoRegional() throws Exception {
        chamar("/checkout/resumo", compra("1", "[{\"sku\":\"CAM-001\",\"quantidade\":1}]", "EXPRESSA", "FRETEGRATIS",
                "BOLETO", 1, "OURO", "NORTE", "01310100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.frete").value(0.00))
                .andExpect(jsonPath("$.prazoEntregaDias").value(5))
                .andExpect(jsonPath("$.seguro").value(2.00))
                .andExpect(jsonPath("$.ajustePagamento").value(3.49))
                .andExpect(jsonPath("$.creditoProximaCompra").value(4.00));
        chamar("/checkout/resumo", compra("1", "[{\"sku\":\"CAM-001\",\"quantidade\":1}]", "ECONOMICA", null,
                "PIX", 1, "BRONZE", "SUL", "01310100"))
                .andExpect(jsonPath("$.frete").value(33.10));
    }

    @Test
    void cartaoParceladoComJuros() throws Exception {
        chamar("/checkout/resumo", compra("1", "[{\"sku\":\"TEN-001\",\"quantidade\":1}]", "RETIRADA_LOJA", null,
                "CARTAO", 6, "BRONZE", "SUDESTE", "01310100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parcelas").value(6));
        chamar("/checkout/resumo", compra("1", "[{\"sku\":\"TEN-001\",\"quantidade\":1}]", "RETIRADA_LOJA", null,
                "CARTAO", 13, "BRONZE", "SUDESTE", "01310100"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.erro").value("PARCELAMENTO_INVALIDO"));
    }

    @Test
    void erros() throws Exception {
        String um = "[{\"sku\":\"CAM-001\",\"quantidade\":1}]";
        chamar("/checkout/resumo", compra("1", "[]", "MOTOBOY", null, "PIX", 1, "OURO", "SUL", "01310100"))
                .andExpect(jsonPath("$.erro").value("PEDIDO_INVALIDO"));
        chamar("/checkout/resumo", compra("1", "[{\"sku\":\"REL-001\",\"quantidade\":1}]", "MOTOBOY", null, "PIX", 1,
                "OURO", "SUL", "01310100")).andExpect(jsonPath("$.erro").value("PRODUTO_FORA_DE_LINHA"));
        chamar("/checkout/resumo", compra("1", "[{\"sku\":\"BON-001\",\"quantidade\":1}]", "MOTOBOY", null, "PIX", 1,
                "OURO", "SUL", "01310100")).andExpect(jsonPath("$.erro").value("ESTOQUE_INSUFICIENTE"));
        chamar("/checkout/resumo", compra("1", um, "ECONOMICA", null, "PIX", 1, "OURO", "SUL", "99999999"))
                .andExpect(jsonPath("$.erro").value("MODALIDADE_INDISPONIVEL"));
        chamar("/checkout/resumo", compra("1", um, "ECONOMICA", null, "PIX", 1, "OURO", "SUL", "88888888"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.erro").value("TRANSPORTADORA_INDISPONIVEL"));
        chamar("/checkout/resumo", compra("1", um, "MOTOBOY", "bemvindo10", "PIX", 1, "OURO", "SUL", "01310100"))
                .andExpect(jsonPath("$.erro").value("CUPOM_INVALIDO"));
        chamar("/checkout/resumo", compra("1", um, "MOTOBOY", "MENOS50", "PIX", 1, "OURO", "SUL", "01310100"))
                .andExpect(jsonPath("$.erro").value("CUPOM_NAO_APLICAVEL"));
        chamar("/checkout/resumo", compra("1", um, "RETIRADA_LOJA", "FRETEGRATIS", "PIX", 1, "OURO", "SUL",
                "01310100")).andExpect(jsonPath("$.erro").value("CUPOM_NAO_APLICAVEL"));
        chamar("/checkout/resumo", compra("1", "[{\"sku\":\"JAQ-001\",\"quantidade\":3}]", "RETIRADA_LOJA", null,
                "BOLETO", 1, "OURO", "SUL", "01310100"))
                .andExpect(jsonPath("$.erro").value("FORMA_PAGAMENTO_INDISPONIVEL"));
    }

    @Test
    void pedidoPagamentoIdempotenteEstoqueECredito() throws Exception {
        String cpf = "55555555555";
        String corpo = compra(cpf, "[{\"sku\":\"MOC-001\",\"quantidade\":1}]", "RETIRADA_LOJA", null, "PIX", 1,
                "PRATA", "SUDESTE", "01310100");
        String resumo = chamar("/checkout/resumo", corpo).andReturn().getResponse().getContentAsString();
        String a = chamar("/pedidos", corpo).andExpect(status().isCreated())
                .andExpect(jsonPath("$.situacao").value("AGUARDANDO_PAGAMENTO"))
                .andReturn().getResponse().getContentAsString();
        String id = a.replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");
        assertEquals(true, a.contains(resumo));
        String pagamento = "{\"idTransacao\":\"TX-1\",\"pagoEm\":\"2026-10-14T16:00:00Z\"}";
        chamar("/pedidos/" + id + "/pagamento", pagamento).andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("PAGO"));
        chamar("/pedidos/" + id + "/pagamento", pagamento).andExpect(status().isOk());
        mvc.perform(get("/produtos/MOC-001")).andExpect(jsonPath("$.estoque").value(0));
        mvc.perform(get("/clientes/" + cpf)).andExpect(jsonPath("$.saldoCredito").value(3.20));
        mvc.perform(get("/clientes/" + cpf + "/avisos")).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].tipo").value("PAGAMENTO_APROVADO"))
                .andExpect(jsonPath("$[0].situacao").value("NOVO"));
        chamar("/pedidos/" + id + "/cancelamento", "{}").andExpect(jsonPath("$.situacao").value("CANCELADO"));
        mvc.perform(get("/produtos/MOC-001")).andExpect(jsonPath("$.estoque").value(1));
        mvc.perform(get("/clientes/" + cpf)).andExpect(jsonPath("$.saldoCredito").value(0.00));
        chamar("/pedidos/" + id + "/envio", "{\"codigoRastreio\":\"X\"}").andExpect(status().isConflict())
                .andExpect(jsonPath("$.erro").value("OPERACAO_NAO_PERMITIDA"));
    }

    @Test
    void pagamentoExpiradoCancela() throws Exception {
        String corpo = compra("77777777777", "[{\"sku\":\"MEI-001\",\"quantidade\":1}]", "RETIRADA_LOJA", null, "PIX",
                1, "BRONZE", "SUDESTE", "01310100");
        String a = chamar("/pedidos", corpo).andReturn().getResponse().getContentAsString();
        String id = a.replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");
        chamar("/pedidos/" + id + "/pagamento", "{\"idTransacao\":\"TX-9\",\"pagoEm\":\"2099-01-01T00:00:00Z\"}")
                .andExpect(status().isConflict()).andExpect(jsonPath("$.erro").value("PEDIDO_EXPIRADO"));
        mvc.perform(get("/pedidos/" + id)).andExpect(jsonPath("$.situacao").value("CANCELADO"));
        mvc.perform(get("/pedidos/inexistente")).andExpect(status().isNotFound());
    }
}
