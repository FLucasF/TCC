package com.loja.checkout;

import static org.assertj.core.api.Assertions.assertThat;

import com.loja.checkout.web.dto.ErroResponse;
import com.loja.checkout.web.dto.ItemRequest;
import com.loja.checkout.web.dto.ResumoRequest;
import com.loja.checkout.web.dto.ResumoResponse;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class CheckoutResumoIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    private static final List<ItemRequest> ITENS_CAMISETA_TENIS = List.of(
            new ItemRequest("Camiseta", 79.90, 2, 0.30),
            new ItemRequest("Tênis", 249.90, 1, 1.20));

    @Test
    void exemplo1_expressaBemvindo10PixBronzeNorte() {
        ResumoRequest request = new ResumoRequest(
                ITENS_CAMISETA_TENIS, "EXPRESSA", "BEMVINDO10", "PIX", null, "BRONZE", "NORTE");

        ResumoResponse resposta = chamar(request);

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
    void exemplo2_economicaSemCupomCartao6xPrataCentroOeste() {
        ResumoRequest request = new ResumoRequest(
                ITENS_CAMISETA_TENIS, "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE");

        ResumoResponse resposta = chamar(request);

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
    void exemplo3_motoboyMenos50BoletoBronzeNordeste() {
        List<ItemRequest> itens = List.of(new ItemRequest("Fone", 199.90, 2, 0.25));
        ResumoRequest request = new ResumoRequest(
                itens, "MOTOBOY", "MENOS50", "BOLETO", null, "BRONZE", "NORDESTE");

        ResumoResponse resposta = chamar(request);

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
    void exemplo4_retiradaLeve3Pague2Cartao3xPrataSul() {
        List<ItemRequest> itens = List.of(
                new ItemRequest("Meia", 19.90, 7, 0.10),
                new ItemRequest("Camiseta", 79.90, 2, 0.30));
        ResumoRequest request = new ResumoRequest(
                itens, "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL");

        ResumoResponse resposta = chamar(request);

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
    void exemplo5_expressaSemCupomPixOuroSudeste() {
        ResumoRequest request = new ResumoRequest(
                ITENS_CAMISETA_TENIS, "EXPRESSA", null, "PIX", null, "OURO", "SUDESTE");

        ResumoResponse resposta = chamar(request);

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
                List.of(), "EXPRESSA", null, "PIX", null, "BRONZE", "SUDESTE");

        ErroResponse erro = chamarComErro(request);

        assertThat(erro.erro()).isEqualTo("PEDIDO_INVALIDO");
    }

    @Test
    void nivelClubeInvalidoRetornaErro() {
        ResumoRequest request = new ResumoRequest(
                ITENS_CAMISETA_TENIS, "EXPRESSA", null, "PIX", null, "DIAMANTE", "SUDESTE");

        ErroResponse erro = chamarComErro(request);

        assertThat(erro.erro()).isEqualTo("NIVEL_CLUBE_INVALIDO");
    }

    @Test
    void motoboyAcimaDoLimiteRetornaModalidadeIndisponivel() {
        List<ItemRequest> itens = List.of(new ItemRequest("Caixa pesada", 100.0, 1, 6.0));
        ResumoRequest request = new ResumoRequest(
                itens, "MOTOBOY", null, "PIX", null, "BRONZE", "SUDESTE");

        ErroResponse erro = chamarComErro(request);

        assertThat(erro.erro()).isEqualTo("MODALIDADE_INDISPONIVEL");
    }

    @Test
    void cupomMenos50AbaixoDoMinimoRetornaCupomNaoAplicavel() {
        List<ItemRequest> itens = List.of(new ItemRequest("Meia", 19.90, 1, 0.10));
        ResumoRequest request = new ResumoRequest(
                itens, "ECONOMICA", "MENOS50", "PIX", null, "BRONZE", "SUDESTE");

        ErroResponse erro = chamarComErro(request);

        assertThat(erro.erro()).isEqualTo("CUPOM_NAO_APLICAVEL");
    }

    @Test
    void boletoAcimaDoLimiteRetornaFormaPagamentoIndisponivel() {
        List<ItemRequest> itens = List.of(new ItemRequest("Notebook", 6000.0, 1, 2.0));
        ResumoRequest request = new ResumoRequest(
                itens, "ECONOMICA", null, "BOLETO", null, "BRONZE", "SUDESTE");

        ErroResponse erro = chamarComErro(request);

        assertThat(erro.erro()).isEqualTo("FORMA_PAGAMENTO_INDISPONIVEL");
    }

    @Test
    void pixComParcelamentoRetornaParcelamentoInvalido() {
        ResumoRequest request = new ResumoRequest(
                ITENS_CAMISETA_TENIS, "EXPRESSA", null, "PIX", 2, "BRONZE", "SUDESTE");

        ErroResponse erro = chamarComErro(request);

        assertThat(erro.erro()).isEqualTo("PARCELAMENTO_INVALIDO");
    }

    private ResumoResponse chamar(ResumoRequest request) {
        ResponseEntity<ResumoResponse> resposta = restTemplate.postForEntity(
                "/checkout/resumo", request, ResumoResponse.class);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        return resposta.getBody();
    }

    private ErroResponse chamarComErro(ResumoRequest request) {
        ResponseEntity<ErroResponse> resposta = restTemplate.postForEntity(
                "/checkout/resumo", request, ErroResponse.class);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        return resposta.getBody();
    }
}
