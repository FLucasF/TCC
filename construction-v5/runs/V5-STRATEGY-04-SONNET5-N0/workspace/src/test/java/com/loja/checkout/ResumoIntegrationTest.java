package com.loja.checkout;

import static org.assertj.core.api.Assertions.assertThat;

import com.loja.checkout.dto.ItemRequest;
import com.loja.checkout.dto.ResumoRequest;
import com.loja.checkout.dto.ResumoResponse;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ResumoIntegrationTest {

    @LocalServerPort
    private int port;

    private RestClient restClient() {
        return RestClient.create("http://localhost:" + port);
    }

    private ResumoResponse chamarOk(ResumoRequest request) {
        return restClient().post()
                .uri("/checkout/resumo")
                .body(request)
                .retrieve()
                .body(ResumoResponse.class);
    }

    private String chamarComErro(ResumoRequest request) {
        try {
            restClient().post()
                    .uri("/checkout/resumo")
                    .body(request)
                    .retrieve()
                    .body(Map.class);
            throw new AssertionError("Era esperado um erro, mas a chamada teve sucesso");
        } catch (HttpClientErrorException ex) {
            assertThat(ex.getStatusCode().value()).isEqualTo(400);
            return ex.getResponseBodyAsString();
        }
    }

    private List<ItemRequest> itensExemploBase() {
        return List.of(
                new ItemRequest("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
                new ItemRequest("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"))
        );
    }

    @Test
    void exemplo1() {
        ResumoRequest request = new ResumoRequest(
                itensExemploBase(), "EXPRESSA", "BEMVINDO10", "PIX", null, "BRONZE", "NORTE"
        );
        ResumoResponse resposta = chamarOk(request);

        assertThat(resposta.subtotalProdutos()).isEqualTo(new BigDecimal("409.70"));
        assertThat(resposta.descontoCupom()).isEqualTo(new BigDecimal("40.97"));
        assertThat(resposta.frete()).isEqualTo(new BigDecimal("33.10"));
        assertThat(resposta.prazoEntregaDias()).isEqualTo(2);
        assertThat(resposta.seguro()).isEqualTo(new BigDecimal("10.24"));
        assertThat(resposta.ajustePagamento()).isEqualTo(new BigDecimal("-20.60"));
        assertThat(resposta.totalFinal()).isEqualTo(new BigDecimal("391.47"));
        assertThat(resposta.parcelas()).isEqualTo(1);
        assertThat(resposta.valorParcela()).isEqualTo(new BigDecimal("391.47"));
        assertThat(resposta.creditoProximaCompra()).isEqualTo(new BigDecimal("0.00"));
        assertThat(resposta.brinde()).isFalse();
    }

    @Test
    void exemplo2() {
        ResumoRequest request = new ResumoRequest(
                itensExemploBase(), "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE"
        );
        ResumoResponse resposta = chamarOk(request);

        assertThat(resposta.subtotalProdutos()).isEqualTo(new BigDecimal("409.70"));
        assertThat(resposta.descontoCupom()).isEqualTo(new BigDecimal("0.00"));
        assertThat(resposta.frete()).isEqualTo(new BigDecimal("15.60"));
        assertThat(resposta.prazoEntregaDias()).isEqualTo(7);
        assertThat(resposta.seguro()).isEqualTo(new BigDecimal("6.15"));
        assertThat(resposta.ajustePagamento()).isEqualTo(new BigDecimal("30.55"));
        assertThat(resposta.totalFinal()).isEqualTo(new BigDecimal("462.00"));
        assertThat(resposta.parcelas()).isEqualTo(6);
        assertThat(resposta.valorParcela()).isEqualTo(new BigDecimal("77.00"));
        assertThat(resposta.creditoProximaCompra()).isEqualTo(new BigDecimal("8.19"));
        assertThat(resposta.brinde()).isFalse();
    }

    @Test
    void exemplo3() {
        List<ItemRequest> itens = List.of(
                new ItemRequest("Fone", new BigDecimal("199.90"), 2, new BigDecimal("0.25"))
        );
        ResumoRequest request = new ResumoRequest(
                itens, "MOTOBOY", "MENOS50", "BOLETO", null, "BRONZE", "NORDESTE"
        );
        ResumoResponse resposta = chamarOk(request);

        assertThat(resposta.subtotalProdutos()).isEqualTo(new BigDecimal("399.80"));
        assertThat(resposta.descontoCupom()).isEqualTo(new BigDecimal("50.00"));
        assertThat(resposta.frete()).isEqualTo(new BigDecimal("18.00"));
        assertThat(resposta.prazoEntregaDias()).isEqualTo(0);
        assertThat(resposta.seguro()).isEqualTo(new BigDecimal("8.00"));
        assertThat(resposta.ajustePagamento()).isEqualTo(new BigDecimal("3.49"));
        assertThat(resposta.totalFinal()).isEqualTo(new BigDecimal("379.29"));
        assertThat(resposta.parcelas()).isEqualTo(1);
        assertThat(resposta.valorParcela()).isEqualTo(new BigDecimal("379.29"));
        assertThat(resposta.creditoProximaCompra()).isEqualTo(new BigDecimal("0.00"));
        assertThat(resposta.brinde()).isFalse();
    }

    @Test
    void exemplo4() {
        List<ItemRequest> itens = List.of(
                new ItemRequest("Meia", new BigDecimal("19.90"), 7, new BigDecimal("0.10")),
                new ItemRequest("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30"))
        );
        ResumoRequest request = new ResumoRequest(
                itens, "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL"
        );
        ResumoResponse resposta = chamarOk(request);

        assertThat(resposta.subtotalProdutos()).isEqualTo(new BigDecimal("299.10"));
        assertThat(resposta.descontoCupom()).isEqualTo(new BigDecimal("39.80"));
        assertThat(resposta.frete()).isEqualTo(new BigDecimal("0.00"));
        assertThat(resposta.prazoEntregaDias()).isEqualTo(1);
        assertThat(resposta.seguro()).isEqualTo(new BigDecimal("2.99"));
        assertThat(resposta.ajustePagamento()).isEqualTo(new BigDecimal("0.00"));
        assertThat(resposta.totalFinal()).isEqualTo(new BigDecimal("262.29"));
        assertThat(resposta.parcelas()).isEqualTo(3);
        assertThat(resposta.valorParcela()).isEqualTo(new BigDecimal("87.43"));
        assertThat(resposta.creditoProximaCompra()).isEqualTo(new BigDecimal("5.98"));
        assertThat(resposta.brinde()).isFalse();
    }

    @Test
    void exemplo5() {
        ResumoRequest request = new ResumoRequest(
                itensExemploBase(), "EXPRESSA", null, "PIX", null, "OURO", "SUDESTE"
        );
        ResumoResponse resposta = chamarOk(request);

        assertThat(resposta.subtotalProdutos()).isEqualTo(new BigDecimal("409.70"));
        assertThat(resposta.descontoCupom()).isEqualTo(new BigDecimal("0.00"));
        assertThat(resposta.frete()).isEqualTo(new BigDecimal("0.00"));
        assertThat(resposta.prazoEntregaDias()).isEqualTo(2);
        assertThat(resposta.seguro()).isEqualTo(new BigDecimal("4.10"));
        assertThat(resposta.ajustePagamento()).isEqualTo(new BigDecimal("-20.69"));
        assertThat(resposta.totalFinal()).isEqualTo(new BigDecimal("393.11"));
        assertThat(resposta.parcelas()).isEqualTo(1);
        assertThat(resposta.valorParcela()).isEqualTo(new BigDecimal("393.11"));
        assertThat(resposta.creditoProximaCompra()).isEqualTo(new BigDecimal("20.48"));
        assertThat(resposta.brinde()).isFalse();
    }

    @Test
    void carrinhoVazioRetornaPedidoInvalido() {
        ResumoRequest request = new ResumoRequest(
                List.of(), "EXPRESSA", null, "PIX", null, "BRONZE", "SUDESTE"
        );
        assertThat(chamarComErro(request)).contains("PEDIDO_INVALIDO");
    }

    @Test
    void nivelClubeInvalido() {
        ResumoRequest request = new ResumoRequest(
                itensExemploBase(), "EXPRESSA", null, "PIX", null, "DIAMANTE", "SUDESTE"
        );
        assertThat(chamarComErro(request)).contains("NIVEL_CLUBE_INVALIDO");
    }

    @Test
    void regiaoInvalida() {
        ResumoRequest request = new ResumoRequest(
                itensExemploBase(), "EXPRESSA", null, "PIX", null, "BRONZE", "MARTE"
        );
        assertThat(chamarComErro(request)).contains("REGIAO_INVALIDA");
    }

    @Test
    void modalidadeInvalida() {
        ResumoRequest request = new ResumoRequest(
                itensExemploBase(), "TELEPORTE", null, "PIX", null, "BRONZE", "SUDESTE"
        );
        assertThat(chamarComErro(request)).contains("MODALIDADE_INVALIDA");
    }

    @Test
    void motoboyAcimaDoLimiteDePesoEhIndisponivel() {
        List<ItemRequest> itens = List.of(
                new ItemRequest("Caixa pesada", new BigDecimal("50.00"), 1, new BigDecimal("6.00"))
        );
        ResumoRequest request = new ResumoRequest(
                itens, "MOTOBOY", null, "PIX", null, "BRONZE", "SUDESTE"
        );
        assertThat(chamarComErro(request)).contains("MODALIDADE_INDISPONIVEL");
    }

    @Test
    void cupomInvalido() {
        ResumoRequest request = new ResumoRequest(
                itensExemploBase(), "EXPRESSA", "NAOEXISTE", "PIX", null, "BRONZE", "SUDESTE"
        );
        assertThat(chamarComErro(request)).contains("CUPOM_INVALIDO");
    }

    @Test
    void cupomNaoAplicavelPorValorMinimo() {
        List<ItemRequest> itens = List.of(
                new ItemRequest("Meia", new BigDecimal("19.90"), 1, new BigDecimal("0.10"))
        );
        ResumoRequest request = new ResumoRequest(
                itens, "EXPRESSA", "MENOS50", "PIX", null, "BRONZE", "SUDESTE"
        );
        assertThat(chamarComErro(request)).contains("CUPOM_NAO_APLICAVEL");
    }

    @Test
    void formaPagamentoInvalida() {
        ResumoRequest request = new ResumoRequest(
                itensExemploBase(), "EXPRESSA", null, "CRIPTO", null, "BRONZE", "SUDESTE"
        );
        assertThat(chamarComErro(request)).contains("FORMA_PAGAMENTO_INVALIDA");
    }

    @Test
    void parcelamentoInvalidoNoPix() {
        ResumoRequest request = new ResumoRequest(
                itensExemploBase(), "EXPRESSA", null, "PIX", 2, "BRONZE", "SUDESTE"
        );
        assertThat(chamarComErro(request)).contains("PARCELAMENTO_INVALIDO");
    }

    @Test
    void parcelamentoInvalidoNoCartaoAlemDe12() {
        ResumoRequest request = new ResumoRequest(
                itensExemploBase(), "EXPRESSA", null, "CARTAO", 13, "BRONZE", "SUDESTE"
        );
        assertThat(chamarComErro(request)).contains("PARCELAMENTO_INVALIDO");
    }

    @Test
    void boletoIndisponivelAcimaDeMilReais() {
        List<ItemRequest> itens = List.of(
                new ItemRequest("Notebook", new BigDecimal("1500.00"), 1, new BigDecimal("2.00"))
        );
        ResumoRequest request = new ResumoRequest(
                itens, "RETIRADA_LOJA", null, "BOLETO", null, "BRONZE", "SUDESTE"
        );
        assertThat(chamarComErro(request)).contains("FORMA_PAGAMENTO_INDISPONIVEL");
    }
}
