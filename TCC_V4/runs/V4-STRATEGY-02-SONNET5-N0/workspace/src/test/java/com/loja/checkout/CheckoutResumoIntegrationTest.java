package com.loja.checkout;

import com.loja.checkout.dto.ErroResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CheckoutResumoIntegrationTest {

    @LocalServerPort
    private int port;

    private final TestRestTemplate restTemplate = new TestRestTemplate();

    private ResponseEntity<String> enviar(String json) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.postForEntity(
                "http://localhost:" + port + "/checkout/resumo",
                new HttpEntity<>(json, headers),
                String.class);
    }

    @Test
    void exemplo1_expressaBemvindo10Pix() {
        String json = """
                {
                  "itens": [
                    {"nome": "Camiseta", "precoUnitario": 79.90, "quantidade": 2, "pesoKg": 0.30},
                    {"nome": "Tenis", "precoUnitario": 249.90, "quantidade": 1, "pesoKg": 1.20}
                  ],
                  "modalidadeEntrega": "EXPRESSA",
                  "cupom": "BEMVINDO10",
                  "formaPagamento": "PIX",
                  "nivelClube": "BRONZE",
                  "regiao": "NORTE"
                }
                """;
        ResponseEntity<String> resposta = enviar(json);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        String body = resposta.getBody();
        assertThat(body).contains("\"subtotalProdutos\":409.70");
        assertThat(body).contains("\"descontoCupom\":40.97");
        assertThat(body).contains("\"frete\":33.10");
        assertThat(body).contains("\"prazoEntregaDias\":2");
        assertThat(body).contains("\"seguro\":10.24");
        assertThat(body).contains("\"ajustePagamento\":-20.60");
        assertThat(body).contains("\"totalFinal\":391.47");
        assertThat(body).contains("\"parcelas\":1");
        assertThat(body).contains("\"valorParcela\":391.47");
        assertThat(body).contains("\"creditoProximaCompra\":0.00");
        assertThat(body).contains("\"brinde\":false");
    }

    @Test
    void exemplo2_economicaCartao6xPrata() {
        String json = """
                {
                  "itens": [
                    {"nome": "Camiseta", "precoUnitario": 79.90, "quantidade": 2, "pesoKg": 0.30},
                    {"nome": "Tenis", "precoUnitario": 249.90, "quantidade": 1, "pesoKg": 1.20}
                  ],
                  "modalidadeEntrega": "ECONOMICA",
                  "formaPagamento": "CARTAO",
                  "parcelas": 6,
                  "nivelClube": "PRATA",
                  "regiao": "CENTRO_OESTE"
                }
                """;
        ResponseEntity<String> resposta = enviar(json);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        String body = resposta.getBody();
        assertThat(body).contains("\"subtotalProdutos\":409.70");
        assertThat(body).contains("\"descontoCupom\":0.00");
        assertThat(body).contains("\"frete\":15.60");
        assertThat(body).contains("\"prazoEntregaDias\":7");
        assertThat(body).contains("\"seguro\":6.15");
        assertThat(body).contains("\"ajustePagamento\":30.55");
        assertThat(body).contains("\"totalFinal\":462.00");
        assertThat(body).contains("\"parcelas\":6");
        assertThat(body).contains("\"valorParcela\":77.00");
        assertThat(body).contains("\"creditoProximaCompra\":8.19");
        assertThat(body).contains("\"brinde\":false");
    }

    @Test
    void exemplo3_motoboyMenos50BoletoBronze() {
        String json = """
                {
                  "itens": [
                    {"nome": "Fone", "precoUnitario": 199.90, "quantidade": 2, "pesoKg": 0.25}
                  ],
                  "modalidadeEntrega": "MOTOBOY",
                  "cupom": "MENOS50",
                  "formaPagamento": "BOLETO",
                  "nivelClube": "BRONZE",
                  "regiao": "NORDESTE"
                }
                """;
        ResponseEntity<String> resposta = enviar(json);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        String body = resposta.getBody();
        assertThat(body).contains("\"subtotalProdutos\":399.80");
        assertThat(body).contains("\"descontoCupom\":50.00");
        assertThat(body).contains("\"frete\":18.00");
        assertThat(body).contains("\"prazoEntregaDias\":0");
        assertThat(body).contains("\"seguro\":8.00");
        assertThat(body).contains("\"ajustePagamento\":3.49");
        assertThat(body).contains("\"totalFinal\":379.29");
        assertThat(body).contains("\"parcelas\":1");
        assertThat(body).contains("\"valorParcela\":379.29");
        assertThat(body).contains("\"creditoProximaCompra\":0.00");
        assertThat(body).contains("\"brinde\":false");
    }

    @Test
    void exemplo4_retiradaLeve3Pague2Cartao3xPrata() {
        String json = """
                {
                  "itens": [
                    {"nome": "Meia", "precoUnitario": 19.90, "quantidade": 7, "pesoKg": 0.10},
                    {"nome": "Camiseta", "precoUnitario": 79.90, "quantidade": 2, "pesoKg": 0.30}
                  ],
                  "modalidadeEntrega": "RETIRADA_LOJA",
                  "cupom": "LEVE3PAGUE2",
                  "formaPagamento": "CARTAO",
                  "parcelas": 3,
                  "nivelClube": "PRATA",
                  "regiao": "SUL"
                }
                """;
        ResponseEntity<String> resposta = enviar(json);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        String body = resposta.getBody();
        assertThat(body).contains("\"subtotalProdutos\":299.10");
        assertThat(body).contains("\"descontoCupom\":39.80");
        assertThat(body).contains("\"frete\":0.00");
        assertThat(body).contains("\"prazoEntregaDias\":1");
        assertThat(body).contains("\"seguro\":2.99");
        assertThat(body).contains("\"ajustePagamento\":0.00");
        assertThat(body).contains("\"totalFinal\":262.29");
        assertThat(body).contains("\"parcelas\":3");
        assertThat(body).contains("\"valorParcela\":87.43");
        assertThat(body).contains("\"creditoProximaCompra\":5.98");
        assertThat(body).contains("\"brinde\":false");
    }

    @Test
    void exemplo5_expressaPixOuroSemCupom() {
        String json = """
                {
                  "itens": [
                    {"nome": "Camiseta", "precoUnitario": 79.90, "quantidade": 2, "pesoKg": 0.30},
                    {"nome": "Tenis", "precoUnitario": 249.90, "quantidade": 1, "pesoKg": 1.20}
                  ],
                  "modalidadeEntrega": "EXPRESSA",
                  "formaPagamento": "PIX",
                  "nivelClube": "OURO",
                  "regiao": "SUDESTE"
                }
                """;
        ResponseEntity<String> resposta = enviar(json);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        String body = resposta.getBody();
        assertThat(body).contains("\"subtotalProdutos\":409.70");
        assertThat(body).contains("\"descontoCupom\":0.00");
        assertThat(body).contains("\"frete\":0.00");
        assertThat(body).contains("\"prazoEntregaDias\":2");
        assertThat(body).contains("\"seguro\":4.10");
        assertThat(body).contains("\"ajustePagamento\":-20.69");
        assertThat(body).contains("\"totalFinal\":393.11");
        assertThat(body).contains("\"parcelas\":1");
        assertThat(body).contains("\"valorParcela\":393.11");
        assertThat(body).contains("\"creditoProximaCompra\":20.48");
        assertThat(body).contains("\"brinde\":false");
    }

    @Test
    void carrinhoVazioRetornaPedidoInvalido() {
        String json = """
                {
                  "itens": [],
                  "modalidadeEntrega": "ECONOMICA",
                  "formaPagamento": "PIX",
                  "nivelClube": "BRONZE",
                  "regiao": "SUL"
                }
                """;
        ResponseEntity<ErroResponse> resposta = restTemplate.postForEntity(
                "http://localhost:" + port + "/checkout/resumo",
                new HttpEntity<>(json, jsonHeaders()),
                ErroResponse.class);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody().erro()).isEqualTo("PEDIDO_INVALIDO");
    }

    @Test
    void motoboyAcimaDoLimiteRetornaModalidadeIndisponivel() {
        String json = """
                {
                  "itens": [
                    {"nome": "Caixa", "precoUnitario": 100.00, "quantidade": 1, "pesoKg": 6.0}
                  ],
                  "modalidadeEntrega": "MOTOBOY",
                  "formaPagamento": "PIX",
                  "nivelClube": "BRONZE",
                  "regiao": "SUL"
                }
                """;
        ResponseEntity<ErroResponse> resposta = restTemplate.postForEntity(
                "http://localhost:" + port + "/checkout/resumo",
                new HttpEntity<>(json, jsonHeaders()),
                ErroResponse.class);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody().erro()).isEqualTo("MODALIDADE_INDISPONIVEL");
    }

    @Test
    void boletoAcimaDoLimiteRetornaFormaPagamentoIndisponivel() {
        String json = """
                {
                  "itens": [
                    {"nome": "Produto", "precoUnitario": 1500.00, "quantidade": 1, "pesoKg": 1.0}
                  ],
                  "modalidadeEntrega": "RETIRADA_LOJA",
                  "formaPagamento": "BOLETO",
                  "nivelClube": "BRONZE",
                  "regiao": "SUL"
                }
                """;
        ResponseEntity<ErroResponse> resposta = restTemplate.postForEntity(
                "http://localhost:" + port + "/checkout/resumo",
                new HttpEntity<>(json, jsonHeaders()),
                ErroResponse.class);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody().erro()).isEqualTo("FORMA_PAGAMENTO_INDISPONIVEL");
    }

    private HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
