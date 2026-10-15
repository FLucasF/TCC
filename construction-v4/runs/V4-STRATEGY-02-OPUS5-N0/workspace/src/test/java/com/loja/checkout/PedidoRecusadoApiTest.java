package com.loja.checkout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** Pedidos recusados: cada situação com o seu código, na ordem de conferência. */
@SpringBootTest
@AutoConfigureMockMvc
class PedidoRecusadoApiTest {

    @Autowired
    private MockMvc mockMvc;

    private void recusa(String pedido, String erroEsperado) throws Exception {
        var resposta = mockMvc.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pedido))
                .andReturn()
                .getResponse();

        assertThat(resposta.getStatus()).isEqualTo(400);
        assertThat(resposta.getContentAsString()).isEqualTo("{\"erro\":\"" + erroEsperado + "\"}");
    }

    private static String pedidoCom(String itens, String resto) {
        return "{\"itens\":" + itens + "," + resto + "}";
    }

    private static final String ITEM_OK =
            "[{\"nome\":\"Camiseta\",\"precoUnitario\":79.90,\"quantidade\":2,\"pesoKg\":0.30}]";
    private static final String RESTO_OK =
            "\"modalidadeEntrega\":\"ECONOMICA\",\"formaPagamento\":\"PIX\","
                    + "\"nivelClube\":\"BRONZE\",\"regiao\":\"SUDESTE\"";

    @Test
    void carrinhoVazio() throws Exception {
        recusa(pedidoCom("[]", RESTO_OK), "PEDIDO_INVALIDO");
    }

    @Test
    void semItens() throws Exception {
        recusa("{" + RESTO_OK + "}", "PEDIDO_INVALIDO");
    }

    @Test
    void itemComPrecoAusente() throws Exception {
        recusa(pedidoCom("[{\"nome\":\"X\",\"quantidade\":1,\"pesoKg\":0.3}]", RESTO_OK),
                "PEDIDO_INVALIDO");
    }

    @Test
    void itemComQuantidadeZero() throws Exception {
        recusa(pedidoCom("[{\"nome\":\"X\",\"precoUnitario\":10.00,\"quantidade\":0,\"pesoKg\":0.3}]",
                RESTO_OK), "PEDIDO_INVALIDO");
    }

    @Test
    void itemComPesoNegativo() throws Exception {
        recusa(pedidoCom("[{\"nome\":\"X\",\"precoUnitario\":10.00,\"quantidade\":1,\"pesoKg\":-0.3}]",
                RESTO_OK), "PEDIDO_INVALIDO");
    }

    @Test
    void itemComPrecoZero() throws Exception {
        recusa(pedidoCom("[{\"nome\":\"X\",\"precoUnitario\":0,\"quantidade\":1,\"pesoKg\":0.3}]",
                RESTO_OK), "PEDIDO_INVALIDO");
    }

    @Test
    void nivelClubeInexistente() throws Exception {
        recusa(pedidoCom(ITEM_OK, "\"modalidadeEntrega\":\"ECONOMICA\",\"formaPagamento\":\"PIX\","
                + "\"nivelClube\":\"DIAMANTE\",\"regiao\":\"SUDESTE\""), "NIVEL_CLUBE_INVALIDO");
    }

    @Test
    void nivelClubeAusente() throws Exception {
        recusa(pedidoCom(ITEM_OK, "\"modalidadeEntrega\":\"ECONOMICA\",\"formaPagamento\":\"PIX\","
                + "\"regiao\":\"SUDESTE\""), "NIVEL_CLUBE_INVALIDO");
    }

    @Test
    void regiaoInexistente() throws Exception {
        recusa(pedidoCom(ITEM_OK, "\"modalidadeEntrega\":\"ECONOMICA\",\"formaPagamento\":\"PIX\","
                + "\"nivelClube\":\"BRONZE\",\"regiao\":\"ASIA\""), "REGIAO_INVALIDA");
    }

    @Test
    void regiaoAusente() throws Exception {
        recusa(pedidoCom(ITEM_OK, "\"modalidadeEntrega\":\"ECONOMICA\",\"formaPagamento\":\"PIX\","
                + "\"nivelClube\":\"BRONZE\""), "REGIAO_INVALIDA");
    }

    @Test
    void modalidadeInexistente() throws Exception {
        recusa(pedidoCom(ITEM_OK, "\"modalidadeEntrega\":\"DRONE\",\"formaPagamento\":\"PIX\","
                + "\"nivelClube\":\"BRONZE\",\"regiao\":\"SUDESTE\""), "MODALIDADE_INVALIDA");
    }

    @Test
    void modalidadeAusente() throws Exception {
        recusa(pedidoCom(ITEM_OK, "\"formaPagamento\":\"PIX\","
                + "\"nivelClube\":\"BRONZE\",\"regiao\":\"SUDESTE\""), "MODALIDADE_INVALIDA");
    }

    @Test
    void motoboyAcimaDeCincoQuilos() throws Exception {
        recusa(pedidoCom("[{\"nome\":\"Mala\",\"precoUnitario\":199.00,\"quantidade\":2,\"pesoKg\":3.00}]",
                "\"modalidadeEntrega\":\"MOTOBOY\",\"formaPagamento\":\"PIX\","
                        + "\"nivelClube\":\"BRONZE\",\"regiao\":\"SUDESTE\""),
                "MODALIDADE_INDISPONIVEL");
    }

    @Test
    void cupomInexistente() throws Exception {
        recusa(pedidoCom(ITEM_OK, RESTO_OK + ",\"cupom\":\"PROMOCAOMALUCA\""), "CUPOM_INVALIDO");
    }

    @Test
    void cupomEmLetraMinusculaNaoVale() throws Exception {
        recusa(pedidoCom(ITEM_OK, RESTO_OK + ",\"cupom\":\"bemvindo10\""), "CUPOM_INVALIDO");
    }

    @Test
    void menos50AbaixoDoMinimo() throws Exception {
        recusa(pedidoCom(ITEM_OK, RESTO_OK + ",\"cupom\":\"MENOS50\""), "CUPOM_NAO_APLICAVEL");
    }

    @Test
    void formaPagamentoInexistente() throws Exception {
        recusa(pedidoCom(ITEM_OK, "\"modalidadeEntrega\":\"ECONOMICA\",\"formaPagamento\":\"CHEQUE\","
                + "\"nivelClube\":\"BRONZE\",\"regiao\":\"SUDESTE\""), "FORMA_PAGAMENTO_INVALIDA");
    }

    @Test
    void formaPagamentoAusente() throws Exception {
        recusa(pedidoCom(ITEM_OK, "\"modalidadeEntrega\":\"ECONOMICA\","
                + "\"nivelClube\":\"BRONZE\",\"regiao\":\"SUDESTE\""), "FORMA_PAGAMENTO_INVALIDA");
    }

    @Test
    void pixParcelado() throws Exception {
        recusa(pedidoCom(ITEM_OK, RESTO_OK + ",\"parcelas\":2"), "PARCELAMENTO_INVALIDO");
    }

    @Test
    void boletoParcelado() throws Exception {
        recusa(pedidoCom(ITEM_OK, "\"modalidadeEntrega\":\"ECONOMICA\",\"formaPagamento\":\"BOLETO\","
                + "\"parcelas\":3,\"nivelClube\":\"BRONZE\",\"regiao\":\"SUDESTE\""),
                "PARCELAMENTO_INVALIDO");
    }

    @Test
    void cartaoAcimaDeDozeVezes() throws Exception {
        recusa(pedidoCom(ITEM_OK, "\"modalidadeEntrega\":\"ECONOMICA\",\"formaPagamento\":\"CARTAO\","
                + "\"parcelas\":13,\"nivelClube\":\"BRONZE\",\"regiao\":\"SUDESTE\""),
                "PARCELAMENTO_INVALIDO");
    }

    @Test
    void cartaoComZeroParcelas() throws Exception {
        recusa(pedidoCom(ITEM_OK, "\"modalidadeEntrega\":\"ECONOMICA\",\"formaPagamento\":\"CARTAO\","
                + "\"parcelas\":0,\"nivelClube\":\"BRONZE\",\"regiao\":\"SUDESTE\""),
                "PARCELAMENTO_INVALIDO");
    }

    @Test
    void boletoAcimaDeMilReais() throws Exception {
        recusa(pedidoCom("[{\"nome\":\"Tenis\",\"precoUnitario\":249.90,\"quantidade\":5,\"pesoKg\":1.20}]",
                "\"modalidadeEntrega\":\"ECONOMICA\",\"formaPagamento\":\"BOLETO\","
                        + "\"nivelClube\":\"BRONZE\",\"regiao\":\"SUDESTE\""),
                "FORMA_PAGAMENTO_INDISPONIVEL");
    }

    @Test
    void corpoMalformadoViraPedidoInvalido() throws Exception {
        recusa("{\"itens\":", "PEDIDO_INVALIDO");
    }

    @Test
    void itemInvalidoVemAntesDoNivelClube() throws Exception {
        recusa(pedidoCom("[]", "\"modalidadeEntrega\":\"DRONE\",\"formaPagamento\":\"CHEQUE\","
                + "\"nivelClube\":\"DIAMANTE\",\"regiao\":\"ASIA\""), "PEDIDO_INVALIDO");
    }

    @Test
    void nivelClubeVemAntesDaRegiao() throws Exception {
        recusa(pedidoCom(ITEM_OK, "\"modalidadeEntrega\":\"DRONE\",\"formaPagamento\":\"CHEQUE\","
                + "\"nivelClube\":\"DIAMANTE\",\"regiao\":\"ASIA\""), "NIVEL_CLUBE_INVALIDO");
    }

    @Test
    void modalidadeVemAntesDoCupom() throws Exception {
        recusa(pedidoCom(ITEM_OK, "\"modalidadeEntrega\":\"DRONE\",\"formaPagamento\":\"CHEQUE\","
                + "\"cupom\":\"NAOEXISTE\",\"nivelClube\":\"BRONZE\",\"regiao\":\"SUDESTE\""),
                "MODALIDADE_INVALIDA");
    }

    @Test
    void cupomVemAntesDaFormaDePagamento() throws Exception {
        recusa(pedidoCom(ITEM_OK, "\"modalidadeEntrega\":\"ECONOMICA\",\"formaPagamento\":\"CHEQUE\","
                + "\"cupom\":\"NAOEXISTE\",\"nivelClube\":\"BRONZE\",\"regiao\":\"SUDESTE\""),
                "CUPOM_INVALIDO");
    }

    @Test
    void parcelamentoVemAntesDaIndisponibilidade() throws Exception {
        recusa(pedidoCom("[{\"nome\":\"Tenis\",\"precoUnitario\":249.90,\"quantidade\":5,\"pesoKg\":1.20}]",
                "\"modalidadeEntrega\":\"ECONOMICA\",\"formaPagamento\":\"BOLETO\",\"parcelas\":2,"
                        + "\"nivelClube\":\"BRONZE\",\"regiao\":\"SUDESTE\""),
                "PARCELAMENTO_INVALIDO");
    }
}
