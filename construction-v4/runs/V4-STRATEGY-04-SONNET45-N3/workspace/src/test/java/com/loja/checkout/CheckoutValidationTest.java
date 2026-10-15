package com.loja.checkout;

import com.loja.checkout.dto.ErroResponse;
import com.loja.checkout.dto.ItemPedido;
import com.loja.checkout.dto.ResumoRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CheckoutValidationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void pedidoInvalido_carrinhoVazio() {
        ResumoRequest request = new ResumoRequest(
            List.of(),
            "ECONOMICA",
            null,
            "PIX",
            null,
            "BRONZE",
            "SUDESTE"
        );

        ResponseEntity<ErroResponse> response = restTemplate.postForEntity(
            "/checkout/resumo", request, ErroResponse.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("PEDIDO_INVALIDO", response.getBody().erro());
    }

    @Test
    void pedidoInvalido_precoZero() {
        ResumoRequest request = new ResumoRequest(
            List.of(new ItemPedido("Produto", BigDecimal.ZERO, 1, new BigDecimal("1.0"))),
            "ECONOMICA",
            null,
            "PIX",
            null,
            "BRONZE",
            "SUDESTE"
        );

        ResponseEntity<ErroResponse> response = restTemplate.postForEntity(
            "/checkout/resumo", request, ErroResponse.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("PEDIDO_INVALIDO", response.getBody().erro());
    }

    @Test
    void nivelClubeInvalido() {
        ResumoRequest request = new ResumoRequest(
            List.of(new ItemPedido("Produto", new BigDecimal("100.00"), 1, new BigDecimal("1.0"))),
            "ECONOMICA",
            null,
            "PIX",
            null,
            "DIAMANTE",
            "SUDESTE"
        );

        ResponseEntity<ErroResponse> response = restTemplate.postForEntity(
            "/checkout/resumo", request, ErroResponse.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("NIVEL_CLUBE_INVALIDO", response.getBody().erro());
    }

    @Test
    void regiaoInvalida() {
        ResumoRequest request = new ResumoRequest(
            List.of(new ItemPedido("Produto", new BigDecimal("100.00"), 1, new BigDecimal("1.0"))),
            "ECONOMICA",
            null,
            "PIX",
            null,
            "BRONZE",
            "EXTERIOR"
        );

        ResponseEntity<ErroResponse> response = restTemplate.postForEntity(
            "/checkout/resumo", request, ErroResponse.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("REGIAO_INVALIDA", response.getBody().erro());
    }

    @Test
    void modalidadeInvalida() {
        ResumoRequest request = new ResumoRequest(
            List.of(new ItemPedido("Produto", new BigDecimal("100.00"), 1, new BigDecimal("1.0"))),
            "DRONE",
            null,
            "PIX",
            null,
            "BRONZE",
            "SUDESTE"
        );

        ResponseEntity<ErroResponse> response = restTemplate.postForEntity(
            "/checkout/resumo", request, ErroResponse.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("MODALIDADE_INVALIDA", response.getBody().erro());
    }

    @Test
    void modalidadeIndisponivel_motoboyAcima5kg() {
        ResumoRequest request = new ResumoRequest(
            List.of(new ItemPedido("Produto Pesado", new BigDecimal("100.00"), 3, new BigDecimal("2.0"))),
            "MOTOBOY",
            null,
            "PIX",
            null,
            "BRONZE",
            "SUDESTE"
        );

        ResponseEntity<ErroResponse> response = restTemplate.postForEntity(
            "/checkout/resumo", request, ErroResponse.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("MODALIDADE_INDISPONIVEL", response.getBody().erro());
    }

    @Test
    void cupomInvalido() {
        ResumoRequest request = new ResumoRequest(
            List.of(new ItemPedido("Produto", new BigDecimal("100.00"), 1, new BigDecimal("1.0"))),
            "ECONOMICA",
            "CUPOMINEXISTENTE",
            "PIX",
            null,
            "BRONZE",
            "SUDESTE"
        );

        ResponseEntity<ErroResponse> response = restTemplate.postForEntity(
            "/checkout/resumo", request, ErroResponse.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("CUPOM_INVALIDO", response.getBody().erro());
    }

    @Test
    void cupomNaoAplicavel_menos50Abaixo300() {
        ResumoRequest request = new ResumoRequest(
            List.of(new ItemPedido("Produto", new BigDecimal("100.00"), 1, new BigDecimal("1.0"))),
            "ECONOMICA",
            "MENOS50",
            "PIX",
            null,
            "BRONZE",
            "SUDESTE"
        );

        ResponseEntity<ErroResponse> response = restTemplate.postForEntity(
            "/checkout/resumo", request, ErroResponse.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("CUPOM_NAO_APLICAVEL", response.getBody().erro());
    }

    @Test
    void formaPagamentoInvalida() {
        ResumoRequest request = new ResumoRequest(
            List.of(new ItemPedido("Produto", new BigDecimal("100.00"), 1, new BigDecimal("1.0"))),
            "ECONOMICA",
            null,
            "BITCOIN",
            null,
            "BRONZE",
            "SUDESTE"
        );

        ResponseEntity<ErroResponse> response = restTemplate.postForEntity(
            "/checkout/resumo", request, ErroResponse.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("FORMA_PAGAMENTO_INVALIDA", response.getBody().erro());
    }

    @Test
    void parcelamentoInvalido_pixMaisDeUmaParcela() {
        ResumoRequest request = new ResumoRequest(
            List.of(new ItemPedido("Produto", new BigDecimal("100.00"), 1, new BigDecimal("1.0"))),
            "ECONOMICA",
            null,
            "PIX",
            3,
            "BRONZE",
            "SUDESTE"
        );

        ResponseEntity<ErroResponse> response = restTemplate.postForEntity(
            "/checkout/resumo", request, ErroResponse.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("PARCELAMENTO_INVALIDO", response.getBody().erro());
    }

    @Test
    void parcelamentoInvalido_cartaoMaisDe12Parcelas() {
        ResumoRequest request = new ResumoRequest(
            List.of(new ItemPedido("Produto", new BigDecimal("100.00"), 1, new BigDecimal("1.0"))),
            "ECONOMICA",
            null,
            "CARTAO",
            15,
            "BRONZE",
            "SUDESTE"
        );

        ResponseEntity<ErroResponse> response = restTemplate.postForEntity(
            "/checkout/resumo", request, ErroResponse.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("PARCELAMENTO_INVALIDO", response.getBody().erro());
    }

    @Test
    void formaPagamentoIndisponivel_boletoAcima1000() {
        ResumoRequest request = new ResumoRequest(
            List.of(new ItemPedido("Produto Caro", new BigDecimal("1200.00"), 1, new BigDecimal("1.0"))),
            "ECONOMICA",
            null,
            "BOLETO",
            null,
            "BRONZE",
            "SUDESTE"
        );

        ResponseEntity<ErroResponse> response = restTemplate.postForEntity(
            "/checkout/resumo", request, ErroResponse.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("FORMA_PAGAMENTO_INDISPONIVEL", response.getBody().erro());
    }
}
