package com.loja.resumo;

import static org.assertj.core.api.Assertions.assertThat;

import com.loja.resumo.dto.ErroResponse;
import com.loja.resumo.dto.ItemRequest;
import com.loja.resumo.dto.ResumoRequest;
import com.loja.resumo.dto.ResumoResponse;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class ResumoCompraIntegrationTest {

    @org.springframework.beans.factory.annotation.Autowired
    private TestRestTemplate restTemplate;

    private static final ItemRequest CAMISETA = new ItemRequest("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30"));
    private static final ItemRequest TENIS = new ItemRequest("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"));
    private static final ItemRequest FONE = new ItemRequest("Fone", new BigDecimal("199.90"), 2, new BigDecimal("0.25"));
    private static final ItemRequest MEIA = new ItemRequest("Meia", new BigDecimal("19.90"), 7, new BigDecimal("0.10"));

    @Test
    void exemplo1() {
        ResumoResponse r = chamar(new ResumoRequest(List.of(CAMISETA, TENIS), "EXPRESSA", "BEMVINDO10", "PIX", null, "BRONZE", "NORTE"));
        assertThat(r.subtotalProdutos()).isEqualByComparingTo("409.70");
        assertThat(r.descontoCupom()).isEqualByComparingTo("40.97");
        assertThat(r.frete()).isEqualByComparingTo("33.10");
        assertThat(r.prazoEntregaDias()).isEqualTo(2);
        assertThat(r.seguro()).isEqualByComparingTo("10.24");
        assertThat(r.ajustePagamento()).isEqualByComparingTo("-20.60");
        assertThat(r.totalFinal()).isEqualByComparingTo("391.47");
        assertThat(r.parcelas()).isEqualTo(1);
        assertThat(r.valorParcela()).isEqualByComparingTo("391.47");
        assertThat(r.creditoProximaCompra()).isEqualByComparingTo("0.00");
        assertThat(r.brinde()).isFalse();
    }

    @Test
    void exemplo2() {
        ResumoResponse r = chamar(new ResumoRequest(List.of(CAMISETA, TENIS), "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE"));
        assertThat(r.subtotalProdutos()).isEqualByComparingTo("409.70");
        assertThat(r.descontoCupom()).isEqualByComparingTo("0.00");
        assertThat(r.frete()).isEqualByComparingTo("15.60");
        assertThat(r.prazoEntregaDias()).isEqualTo(7);
        assertThat(r.seguro()).isEqualByComparingTo("6.15");
        assertThat(r.ajustePagamento()).isEqualByComparingTo("30.55");
        assertThat(r.totalFinal()).isEqualByComparingTo("462.00");
        assertThat(r.parcelas()).isEqualTo(6);
        assertThat(r.valorParcela()).isEqualByComparingTo("77.00");
        assertThat(r.creditoProximaCompra()).isEqualByComparingTo("8.19");
        assertThat(r.brinde()).isFalse();
    }

    @Test
    void exemplo3() {
        ResumoResponse r = chamar(new ResumoRequest(List.of(FONE), "MOTOBOY", "MENOS50", "BOLETO", null, "BRONZE", "NORDESTE"));
        assertThat(r.subtotalProdutos()).isEqualByComparingTo("399.80");
        assertThat(r.descontoCupom()).isEqualByComparingTo("50.00");
        assertThat(r.frete()).isEqualByComparingTo("18.00");
        assertThat(r.prazoEntregaDias()).isEqualTo(0);
        assertThat(r.seguro()).isEqualByComparingTo("8.00");
        assertThat(r.ajustePagamento()).isEqualByComparingTo("3.49");
        assertThat(r.totalFinal()).isEqualByComparingTo("379.29");
        assertThat(r.parcelas()).isEqualTo(1);
        assertThat(r.valorParcela()).isEqualByComparingTo("379.29");
        assertThat(r.creditoProximaCompra()).isEqualByComparingTo("0.00");
        assertThat(r.brinde()).isFalse();
    }

    @Test
    void exemplo4() {
        ResumoResponse r = chamar(new ResumoRequest(List.of(MEIA, CAMISETA), "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL"));
        assertThat(r.subtotalProdutos()).isEqualByComparingTo("299.10");
        assertThat(r.descontoCupom()).isEqualByComparingTo("39.80");
        assertThat(r.frete()).isEqualByComparingTo("0.00");
        assertThat(r.prazoEntregaDias()).isEqualTo(1);
        assertThat(r.seguro()).isEqualByComparingTo("2.99");
        assertThat(r.ajustePagamento()).isEqualByComparingTo("0.00");
        assertThat(r.totalFinal()).isEqualByComparingTo("262.29");
        assertThat(r.parcelas()).isEqualTo(3);
        assertThat(r.valorParcela()).isEqualByComparingTo("87.43");
        assertThat(r.creditoProximaCompra()).isEqualByComparingTo("5.98");
        assertThat(r.brinde()).isFalse();
    }

    @Test
    void exemplo5() {
        ResumoResponse r = chamar(new ResumoRequest(List.of(CAMISETA, TENIS), "EXPRESSA", null, "PIX", null, "OURO", "SUDESTE"));
        assertThat(r.subtotalProdutos()).isEqualByComparingTo("409.70");
        assertThat(r.descontoCupom()).isEqualByComparingTo("0.00");
        assertThat(r.frete()).isEqualByComparingTo("0.00");
        assertThat(r.prazoEntregaDias()).isEqualTo(2);
        assertThat(r.seguro()).isEqualByComparingTo("4.10");
        assertThat(r.ajustePagamento()).isEqualByComparingTo("-20.69");
        assertThat(r.totalFinal()).isEqualByComparingTo("393.11");
        assertThat(r.parcelas()).isEqualTo(1);
        assertThat(r.valorParcela()).isEqualByComparingTo("393.11");
        assertThat(r.creditoProximaCompra()).isEqualByComparingTo("20.48");
        assertThat(r.brinde()).isFalse();
    }

    @Test
    void motoboyAcimaDoLimiteDeveRecusar() {
        ItemRequest pesado = new ItemRequest("Caixa", new BigDecimal("100.00"), 1, new BigDecimal("6.00"));
        ResponseEntity<ErroResponse> resposta = restTemplate.postForEntity(
                "/checkout/resumo",
                new ResumoRequest(List.of(pesado), "MOTOBOY", null, "PIX", null, "BRONZE", "SUDESTE"),
                ErroResponse.class);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody().erro()).isEqualTo("MODALIDADE_INDISPONIVEL");
    }

    @Test
    void carrinhoVazioDeveRecusar() {
        ResponseEntity<ErroResponse> resposta = restTemplate.postForEntity(
                "/checkout/resumo",
                new ResumoRequest(List.of(), "EXPRESSA", null, "PIX", null, "BRONZE", "SUDESTE"),
                ErroResponse.class);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody().erro()).isEqualTo("PEDIDO_INVALIDO");
    }

    @Test
    void boletoAcimaDoLimiteDeveRecusar() {
        ItemRequest caro = new ItemRequest("Notebook", new BigDecimal("1500.00"), 1, new BigDecimal("2.00"));
        ResponseEntity<ErroResponse> resposta = restTemplate.postForEntity(
                "/checkout/resumo",
                new ResumoRequest(List.of(caro), "RETIRADA_LOJA", null, "BOLETO", null, "BRONZE", "SUDESTE"),
                ErroResponse.class);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody().erro()).isEqualTo("FORMA_PAGAMENTO_INDISPONIVEL");
    }

    private ResumoResponse chamar(ResumoRequest request) {
        ResponseEntity<ResumoResponse> resposta = restTemplate.postForEntity("/checkout/resumo", request, ResumoResponse.class);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        return resposta.getBody();
    }
}
