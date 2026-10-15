package com.loja.checkout;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class CheckoutIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String url() {
        return "http://localhost:" + port + "/checkout/resumo";
    }

    private Map<String, Object> item(String nome, String precoUnitario, int quantidade, String pesoKg) {
        return Map.of(
                "nome", nome,
                "precoUnitario", new BigDecimal(precoUnitario),
                "quantidade", quantidade,
                "pesoKg", new BigDecimal(pesoKg)
        );
    }

    private BigDecimal valor(Map<String, Object> body, String campo) {
        return new BigDecimal(body.get(campo).toString());
    }

    @Test
    void exemplo1_expressaBemvindo10PixBronzeNorte() {
        Map<String, Object> corpo = Map.of(
                "itens", List.of(item("Camiseta", "79.90", 2, "0.30"), item("Tenis", "249.90", 1, "1.20")),
                "modalidadeEntrega", "EXPRESSA",
                "cupom", "BEMVINDO10",
                "formaPagamento", "PIX",
                "nivelClube", "BRONZE",
                "regiao", "NORTE"
        );

        ResponseEntity<Map> resposta = restTemplate.postForEntity(url(), corpo, Map.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> body = resposta.getBody();
        assertThat(valor(body, "subtotalProdutos")).isEqualByComparingTo("409.70");
        assertThat(valor(body, "descontoCupom")).isEqualByComparingTo("40.97");
        assertThat(valor(body, "frete")).isEqualByComparingTo("33.10");
        assertThat(body.get("prazoEntregaDias")).isEqualTo(2);
        assertThat(valor(body, "seguro")).isEqualByComparingTo("10.24");
        assertThat(valor(body, "ajustePagamento")).isEqualByComparingTo("-20.60");
        assertThat(valor(body, "totalFinal")).isEqualByComparingTo("391.47");
        assertThat(body.get("parcelas")).isEqualTo(1);
        assertThat(valor(body, "valorParcela")).isEqualByComparingTo("391.47");
        assertThat(valor(body, "creditoProximaCompra")).isEqualByComparingTo("0.00");
        assertThat(body.get("brinde")).isEqualTo(false);
    }

    @Test
    void exemplo2_economicaSemCupomCartao6xPrataCentroOeste() {
        Map<String, Object> corpo = Map.of(
                "itens", List.of(item("Camiseta", "79.90", 2, "0.30"), item("Tenis", "249.90", 1, "1.20")),
                "modalidadeEntrega", "ECONOMICA",
                "formaPagamento", "CARTAO",
                "parcelas", 6,
                "nivelClube", "PRATA",
                "regiao", "CENTRO_OESTE"
        );

        ResponseEntity<Map> resposta = restTemplate.postForEntity(url(), corpo, Map.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> body = resposta.getBody();
        assertThat(valor(body, "subtotalProdutos")).isEqualByComparingTo("409.70");
        assertThat(valor(body, "descontoCupom")).isEqualByComparingTo("0.00");
        assertThat(valor(body, "frete")).isEqualByComparingTo("15.60");
        assertThat(body.get("prazoEntregaDias")).isEqualTo(7);
        assertThat(valor(body, "seguro")).isEqualByComparingTo("6.15");
        assertThat(valor(body, "ajustePagamento")).isEqualByComparingTo("30.55");
        assertThat(valor(body, "totalFinal")).isEqualByComparingTo("462.00");
        assertThat(body.get("parcelas")).isEqualTo(6);
        assertThat(valor(body, "valorParcela")).isEqualByComparingTo("77.00");
        assertThat(valor(body, "creditoProximaCompra")).isEqualByComparingTo("8.19");
        assertThat(body.get("brinde")).isEqualTo(false);
    }

    @Test
    void exemplo3_motoboyMenos50BoletoBronzeNordeste() {
        Map<String, Object> corpo = Map.of(
                "itens", List.of(item("Fone", "199.90", 2, "0.25")),
                "modalidadeEntrega", "MOTOBOY",
                "cupom", "MENOS50",
                "formaPagamento", "BOLETO",
                "nivelClube", "BRONZE",
                "regiao", "NORDESTE"
        );

        ResponseEntity<Map> resposta = restTemplate.postForEntity(url(), corpo, Map.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> body = resposta.getBody();
        assertThat(valor(body, "subtotalProdutos")).isEqualByComparingTo("399.80");
        assertThat(valor(body, "descontoCupom")).isEqualByComparingTo("50.00");
        assertThat(valor(body, "frete")).isEqualByComparingTo("18.00");
        assertThat(body.get("prazoEntregaDias")).isEqualTo(0);
        assertThat(valor(body, "seguro")).isEqualByComparingTo("8.00");
        assertThat(valor(body, "ajustePagamento")).isEqualByComparingTo("3.49");
        assertThat(valor(body, "totalFinal")).isEqualByComparingTo("379.29");
        assertThat(body.get("parcelas")).isEqualTo(1);
        assertThat(valor(body, "valorParcela")).isEqualByComparingTo("379.29");
        assertThat(valor(body, "creditoProximaCompra")).isEqualByComparingTo("0.00");
        assertThat(body.get("brinde")).isEqualTo(false);
    }

    @Test
    void exemplo4_retiradaLeve3Pague2Cartao3xPrataSul() {
        Map<String, Object> corpo = Map.of(
                "itens", List.of(item("Meia", "19.90", 7, "0.10"), item("Camiseta", "79.90", 2, "0.30")),
                "modalidadeEntrega", "RETIRADA_LOJA",
                "cupom", "LEVE3PAGUE2",
                "formaPagamento", "CARTAO",
                "parcelas", 3,
                "nivelClube", "PRATA",
                "regiao", "SUL"
        );

        ResponseEntity<Map> resposta = restTemplate.postForEntity(url(), corpo, Map.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> body = resposta.getBody();
        assertThat(valor(body, "subtotalProdutos")).isEqualByComparingTo("299.10");
        assertThat(valor(body, "descontoCupom")).isEqualByComparingTo("39.80");
        assertThat(valor(body, "frete")).isEqualByComparingTo("0.00");
        assertThat(body.get("prazoEntregaDias")).isEqualTo(1);
        assertThat(valor(body, "seguro")).isEqualByComparingTo("2.99");
        assertThat(valor(body, "ajustePagamento")).isEqualByComparingTo("0.00");
        assertThat(valor(body, "totalFinal")).isEqualByComparingTo("262.29");
        assertThat(body.get("parcelas")).isEqualTo(3);
        assertThat(valor(body, "valorParcela")).isEqualByComparingTo("87.43");
        assertThat(valor(body, "creditoProximaCompra")).isEqualByComparingTo("5.98");
        assertThat(body.get("brinde")).isEqualTo(false);
    }

    @Test
    void exemplo5_expressaSemCupomPixOuroSudeste() {
        Map<String, Object> corpo = Map.of(
                "itens", List.of(item("Camiseta", "79.90", 2, "0.30"), item("Tenis", "249.90", 1, "1.20")),
                "modalidadeEntrega", "EXPRESSA",
                "formaPagamento", "PIX",
                "nivelClube", "OURO",
                "regiao", "SUDESTE"
        );

        ResponseEntity<Map> resposta = restTemplate.postForEntity(url(), corpo, Map.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> body = resposta.getBody();
        assertThat(valor(body, "subtotalProdutos")).isEqualByComparingTo("409.70");
        assertThat(valor(body, "descontoCupom")).isEqualByComparingTo("0.00");
        assertThat(valor(body, "frete")).isEqualByComparingTo("0.00");
        assertThat(body.get("prazoEntregaDias")).isEqualTo(2);
        assertThat(valor(body, "seguro")).isEqualByComparingTo("4.10");
        assertThat(valor(body, "ajustePagamento")).isEqualByComparingTo("-20.69");
        assertThat(valor(body, "totalFinal")).isEqualByComparingTo("393.11");
        assertThat(body.get("parcelas")).isEqualTo(1);
        assertThat(valor(body, "valorParcela")).isEqualByComparingTo("393.11");
        assertThat(valor(body, "creditoProximaCompra")).isEqualByComparingTo("20.48");
        assertThat(body.get("brinde")).isEqualTo(false);
    }

    @Test
    void carrinhoVazioRetornaPedidoInvalido() {
        Map<String, Object> corpo = Map.of(
                "itens", List.of(),
                "modalidadeEntrega", "EXPRESSA",
                "formaPagamento", "PIX",
                "nivelClube", "BRONZE",
                "regiao", "SUDESTE"
        );

        ResponseEntity<Map> resposta = restTemplate.postForEntity(url(), corpo, Map.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(resposta.getBody().get("erro")).isEqualTo("PEDIDO_INVALIDO");
    }

    @Test
    void motoboyAcimaDoLimiteRetornaModalidadeIndisponivel() {
        Map<String, Object> corpo = Map.of(
                "itens", List.of(item("Caixa pesada", "100.00", 1, "6.00")),
                "modalidadeEntrega", "MOTOBOY",
                "formaPagamento", "PIX",
                "nivelClube", "BRONZE",
                "regiao", "SUDESTE"
        );

        ResponseEntity<Map> resposta = restTemplate.postForEntity(url(), corpo, Map.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(resposta.getBody().get("erro")).isEqualTo("MODALIDADE_INDISPONIVEL");
    }

    @Test
    void boletoAcimaDeMilRetornaFormaPagamentoIndisponivel() {
        Map<String, Object> corpo = Map.of(
                "itens", List.of(item("Notebook", "2000.00", 1, "2.00")),
                "modalidadeEntrega", "RETIRADA_LOJA",
                "formaPagamento", "BOLETO",
                "nivelClube", "BRONZE",
                "regiao", "SUDESTE"
        );

        ResponseEntity<Map> resposta = restTemplate.postForEntity(url(), corpo, Map.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(resposta.getBody().get("erro")).isEqualTo("FORMA_PAGAMENTO_INDISPONIVEL");
    }

    @Test
    void cartaoComQuinzeParcelasRetornaParcelamentoInvalido() {
        Map<String, Object> corpo = Map.of(
                "itens", List.of(item("Camiseta", "79.90", 1, "0.30")),
                "modalidadeEntrega", "RETIRADA_LOJA",
                "formaPagamento", "CARTAO",
                "parcelas", 15,
                "nivelClube", "BRONZE",
                "regiao", "SUDESTE"
        );

        ResponseEntity<Map> resposta = restTemplate.postForEntity(url(), corpo, Map.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(resposta.getBody().get("erro")).isEqualTo("PARCELAMENTO_INVALIDO");
    }

    @Test
    void menos50AbaixoDoMinimoRetornaCupomNaoAplicavel() {
        Map<String, Object> corpo = Map.of(
                "itens", List.of(item("Camiseta", "79.90", 1, "0.30")),
                "modalidadeEntrega", "RETIRADA_LOJA",
                "cupom", "MENOS50",
                "formaPagamento", "PIX",
                "nivelClube", "BRONZE",
                "regiao", "SUDESTE"
        );

        ResponseEntity<Map> resposta = restTemplate.postForEntity(url(), corpo, Map.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(resposta.getBody().get("erro")).isEqualTo("CUPOM_NAO_APLICAVEL");
    }
}
