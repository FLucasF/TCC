package com.loja.pedidos;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class PedidoFluxoTest {

    @Autowired
    private TestRestTemplate rest;

    private String criarPedido(double valorProdutos, double frete) {
        Map<String, Object> body = Map.of("valorProdutos", valorProdutos, "frete", frete);
        ResponseEntity<Map> resposta = rest.postForEntity("/pedidos", body, Map.class);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resposta.getBody().get("situacao")).isEqualTo("AGUARDANDO_PAGAMENTO");
        return (String) resposta.getBody().get("id");
    }

    private Map<String, Object> agir(String id, String acao) {
        ResponseEntity<Map> resposta = rest.postForEntity("/pedidos/" + id + "/acoes", Map.of("acao", acao),
                Map.class);
        return resposta.getBody();
    }

    private ResponseEntity<Map> agirResposta(String id, String acao) {
        return rest.postForEntity("/pedidos/" + id + "/acoes", Map.of("acao", acao), Map.class);
    }

    @Test
    void exemplo1_cancelarEmSeparacao() {
        String id = criarPedido(200.00, 20.00);
        agir(id, "PAGAR");
        agir(id, "SEPARAR");
        Map<String, Object> resultado = agir(id, "CANCELAR");

        assertThat(resultado.get("situacao")).isEqualTo("CANCELADO");
        assertThat((Number) resultado.get("valorTotal")).isEqualTo(220.00);
        assertThat((Number) resultado.get("valorReembolsado")).isEqualTo(205.00);
        assertThat(resultado.get("estoqueDevolvido")).isEqualTo(true);
        assertThat(resultado.get("coletaAgendada")).isEqualTo(false);
        assertThat(resultado.get("historico")).isEqualTo(
                java.util.List.of("AGUARDANDO_PAGAMENTO", "PAGO", "EM_SEPARACAO", "CANCELADO"));
    }

    @Test
    void exemplo2_devolucaoAposEntrega() {
        String id = criarPedido(150.00, 12.50);
        agir(id, "PAGAR");
        agir(id, "SEPARAR");
        agir(id, "ENVIAR");
        agir(id, "ENTREGAR");
        Map<String, Object> resultado = agir(id, "DEVOLVER");

        assertThat(resultado.get("situacao")).isEqualTo("DEVOLVIDO");
        assertThat((Number) resultado.get("valorTotal")).isEqualTo(162.50);
        assertThat((Number) resultado.get("valorReembolsado")).isEqualTo(150.00);
        assertThat(resultado.get("estoqueDevolvido")).isEqualTo(false);
        assertThat(resultado.get("coletaAgendada")).isEqualTo(true);
    }

    @Test
    void exemplo3_cancelarSemFrete() {
        String id = criarPedido(80.00, 0.00);
        Map<String, Object> resultado = agir(id, "CANCELAR");

        assertThat(resultado.get("situacao")).isEqualTo("CANCELADO");
        assertThat((Number) resultado.get("valorReembolsado")).isEqualTo(0.00);
        assertThat(resultado.get("estoqueDevolvido")).isEqualTo(false);
        assertThat(resultado.get("coletaAgendada")).isEqualTo(false);
    }

    @Test
    void exemplo4_cancelarPago() {
        String id = criarPedido(250.00, 25.00);
        agir(id, "PAGAR");
        Map<String, Object> resultado = agir(id, "CANCELAR");

        assertThat(resultado.get("situacao")).isEqualTo("CANCELADO");
        assertThat((Number) resultado.get("valorReembolsado")).isEqualTo(275.00);
        assertThat(resultado.get("estoqueDevolvido")).isEqualTo(false);
    }

    @Test
    void exemplo5_taxaMaiorQueTotal() {
        String id = criarPedido(10.00, 3.00);
        agir(id, "PAGAR");
        agir(id, "SEPARAR");
        Map<String, Object> resultado = agir(id, "CANCELAR");

        assertThat(resultado.get("situacao")).isEqualTo("CANCELADO");
        assertThat((Number) resultado.get("valorTotal")).isEqualTo(13.00);
        assertThat((Number) resultado.get("valorReembolsado")).isEqualTo(0.00);
        assertThat(resultado.get("estoqueDevolvido")).isEqualTo(true);
    }

    @Test
    void exemplo6_enviarSemSeparar() {
        String id = criarPedido(99.90, 15.00);
        agir(id, "PAGAR");
        ResponseEntity<Map> resposta = agirResposta(id, "ENVIAR");

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resposta.getBody().get("erro")).isEqualTo("ACAO_NAO_PERMITIDA");

        ResponseEntity<Map> consulta = rest.getForEntity("/pedidos/" + id, Map.class);
        assertThat(consulta.getBody().get("situacao")).isEqualTo("PAGO");
    }

    @Test
    void exemplo7_cancelarEnviado() {
        String id = criarPedido(100.00, 10.00);
        agir(id, "PAGAR");
        agir(id, "SEPARAR");
        agir(id, "ENVIAR");
        ResponseEntity<Map> resposta = agirResposta(id, "CANCELAR");

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resposta.getBody().get("erro")).isEqualTo("ACAO_NAO_PERMITIDA");

        ResponseEntity<Map> consulta = rest.getForEntity("/pedidos/" + id, Map.class);
        assertThat(consulta.getBody().get("situacao")).isEqualTo("ENVIADO");
    }

    @Test
    void exemplo8_acaoInvalida() {
        String id = criarPedido(50.00, 5.00);
        ResponseEntity<Map> resposta = agirResposta(id, "TROCAR");

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody().get("erro")).isEqualTo("ACAO_INVALIDA");
    }

    @Test
    void criarPedidoInvalido_produtosZerado() {
        Map<String, Object> body = Map.of("valorProdutos", 0.0, "frete", 10.0);
        ResponseEntity<Map> resposta = rest.postForEntity("/pedidos", body, Map.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody().get("erro")).isEqualTo("PEDIDO_INVALIDO");
    }

    @Test
    void criarPedidoInvalido_freteNegativo() {
        Map<String, Object> body = Map.of("valorProdutos", 10.0, "frete", -1.0);
        ResponseEntity<Map> resposta = rest.postForEntity("/pedidos", body, Map.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody().get("erro")).isEqualTo("PEDIDO_INVALIDO");
    }

    @Test
    void pedidoNaoEncontrado() {
        ResponseEntity<Map> resposta = rest.getForEntity("/pedidos/inexistente", Map.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resposta.getBody().get("erro")).isEqualTo("PEDIDO_NAO_ENCONTRADO");
    }

    @Test
    void descricoesParaOCliente() {
        String id = criarPedido(30.00, 5.00);
        assertThat(agir(id, "PAGAR").get("descricao")).isEqualTo("Pagamento confirmado");
        assertThat(agir(id, "SEPARAR").get("descricao")).isEqualTo("Separando seus produtos");
        assertThat(agir(id, "ENVIAR").get("descricao")).isEqualTo("A caminho");
        assertThat(agir(id, "ENTREGAR").get("descricao")).isEqualTo("Entregue");
        assertThat(agir(id, "DEVOLVER").get("descricao")).isEqualTo("Devolvido");
    }
}
