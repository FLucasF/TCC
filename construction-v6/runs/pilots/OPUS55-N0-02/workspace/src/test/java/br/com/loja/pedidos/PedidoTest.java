package br.com.loja.pedidos;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

class PedidoTest extends TesteIntegracao {

    private static final String CPF = "12345678909";

    private static void erro(MvcResult resultado, int status, String codigo) throws Exception {
        assertThat(corpo(resultado)).isEqualTo("{\"erro\":\"" + codigo + "\"}");
        assertThat(resultado.getResponse().getStatus()).isEqualTo(status);
    }

    private static void ok(MvcResult resultado) throws Exception {
        assertThat(resultado.getResponse().getStatus()).as(corpo(resultado)).isEqualTo(200);
    }

    private String situacao(long id) throws Exception {
        return ler(consultar("/pedidos/" + id), "$.situacao");
    }

    private String saldo(String cpf) throws Exception {
        String json = corpo(consultar("/clientes/" + cpf));
        return json.replaceAll(".*\"saldoCredito\":([-0-9.]+).*", "$1");
    }

    private List<String> avisos(String cpf) throws Exception {
        List<String> tipos = ler(consultar("/clientes/" + cpf + "/avisos"), "$[*].tipo");
        return tipos;
    }

    @Test
    void registroGuardaOMesmoResumoMostradoAntes() throws Exception {
        Compra compra = compra(CPF, "TEN-001", "1", "CAM-001", "2").entrega("EXPRESSA").regiao("NORDESTE")
                .pagamento("CARTAO").parcelas(5).nivel("OURO").cupom("BEMVINDO10");
        String resumo = corpo(postar("/checkout/resumo", compra.json()));
        MvcResult registro = postar("/pedidos", compra.json());

        assertThat(registro.getResponse().getStatus()).isEqualTo(201);
        String pedido = corpo(registro);
        assertThat(pedido).contains("\"situacao\":\"AGUARDANDO_PAGAMENTO\"")
                .contains("\"resumo\":" + resumo + ",")
                .contains("\"itens\":[{\"sku\":\"CAM-001\",\"quantidade\":2},{\"sku\":\"TEN-001\",\"quantidade\":1}]")
                .contains("\"codigoRastreio\":null");
        String criadoEm = ler(registro, "$.criadoEm");
        assertThat(criadoEm).matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z");
        long id = ((Number) ler(registro, "$.id")).longValue();
        assertThat(corpo(consultar("/pedidos/" + id))).isEqualTo(pedido);

        // registrar não mexe no estoque e cria o cliente com saldo zero
        assertThat(estoque("TEN-001")).isEqualTo(10);
        assertThat(corpo(consultar("/clientes/" + CPF))).isEqualTo("{\"cpf\":\"" + CPF + "\",\"saldoCredito\":0.00}");
    }

    @Test
    void pagamentoBaixaEstoqueDaCreditoEAvisa() throws Exception {
        long id = registrar(compra(CPF, "CAM-001", "2", "MEI-001", "1").nivel("PRATA"));
        // crédito: 2% de 179.70 = 3.594 -> 3.59
        MvcResult pago = pagarAgora(id, "TX-1");
        ok(pago);
        assertThat((String) ler(pago, "$.situacao")).isEqualTo("PAGO");
        assertThat(estoque("CAM-001")).isEqualTo(48);
        assertThat(estoque("MEI-001")).isEqualTo(99);
        assertThat(saldo(CPF)).isEqualTo("3.59");
        assertThat(avisos(CPF)).containsExactly("PAGAMENTO_APROVADO");

        // o banco repete o mesmo aviso: nada muda
        MvcResult repetido = pagarAgora(id, "TX-1");
        ok(repetido);
        assertThat(corpo(repetido)).isEqualTo(corpo(pago));
        assertThat(estoque("CAM-001")).isEqualTo(48);
        assertThat(saldo(CPF)).isEqualTo("3.59");
        assertThat(avisos(CPF)).containsExactly("PAGAMENTO_APROVADO");

        // outra transação para um pedido já pago não é aceita
        erro(pagarAgora(id, "TX-2"), 409, "OPERACAO_NAO_PERMITIDA");
    }

    @Test
    void envioEEntrega() throws Exception {
        long id = registrar(compra(CPF, "CAM-001", "1"));
        erro(postar("/pedidos/" + id + "/envio", "{\"codigoRastreio\": \"BR123456789\"}"), 409, "OPERACAO_NAO_PERMITIDA");
        erro(postar("/pedidos/" + id + "/entrega"), 409, "OPERACAO_NAO_PERMITIDA");
        ok(pagarAgora(id, "TX-1"));
        erro(postar("/pedidos/" + id + "/entrega"), 409, "OPERACAO_NAO_PERMITIDA");

        MvcResult enviado = postar("/pedidos/" + id + "/envio", "{\"codigoRastreio\": \"BR123456789\"}");
        ok(enviado);
        assertThat((String) ler(enviado, "$.situacao")).isEqualTo("ENVIADO");
        assertThat((String) ler(enviado, "$.codigoRastreio")).isEqualTo("BR123456789");
        erro(postar("/pedidos/" + id + "/cancelamento"), 409, "OPERACAO_NAO_PERMITIDA");

        MvcResult entregue = postar("/pedidos/" + id + "/entrega");
        ok(entregue);
        assertThat((String) ler(entregue, "$.situacao")).isEqualTo("ENTREGUE");
        erro(postar("/pedidos/" + id + "/cancelamento"), 409, "OPERACAO_NAO_PERMITIDA");
        assertThat(avisos(CPF)).containsExactly("PAGAMENTO_APROVADO", "PEDIDO_ENVIADO");
    }

    @Test
    void cancelarPedidoPagoDevolveEstoqueECredito() throws Exception {
        long id = registrar(compra(CPF, "JAQ-001", "2").nivel("OURO").pagamento("CARTAO"));
        ok(pagarAgora(id, "TX-1"));
        assertThat(estoque("JAQ-001")).isEqualTo(1);
        assertThat(saldo(CPF)).isEqualTo("38.99");

        MvcResult cancelado = postar("/pedidos/" + id + "/cancelamento");
        ok(cancelado);
        assertThat((String) ler(cancelado, "$.situacao")).isEqualTo("CANCELADO");
        assertThat(estoque("JAQ-001")).isEqualTo(3);
        assertThat(saldo(CPF)).isEqualTo("0.00");
        assertThat(avisos(CPF)).containsExactly("PAGAMENTO_APROVADO", "PEDIDO_CANCELADO");
        erro(postar("/pedidos/" + id + "/cancelamento"), 409, "OPERACAO_NAO_PERMITIDA");
    }

    @Test
    void cancelarPedidoNaoPagoSoAvisa() throws Exception {
        long id = registrar(compra(CPF, "CAM-001", "1").nivel("OURO"));
        ok(postar("/pedidos/" + id + "/cancelamento"));
        assertThat(estoque("CAM-001")).isEqualTo(50);
        assertThat(saldo(CPF)).isEqualTo("0.00");
        assertThat(avisos(CPF)).containsExactly("PEDIDO_CANCELADO");
        erro(pagarAgora(id, "TX-1"), 409, "OPERACAO_NAO_PERMITIDA");
    }

    @Test
    void pagamentoComExatamente48HorasAindaVale() throws Exception {
        long id = registrar(compra(CPF, "CAM-001", "1"));
        Instant criadoEm = Instant.parse(ler(consultar("/pedidos/" + id), "$.criadoEm"));
        ok(pagar(id, "TX-1", criadoEm.plusSeconds(48 * 3600).toString()));
    }

    @Test
    void pagamentoDepoisDe48HorasCancelaOPedido() throws Exception {
        long id = registrar(compra(CPF, "CAM-001", "1"));
        Instant criadoEm = Instant.parse(ler(consultar("/pedidos/" + id), "$.criadoEm"));
        erro(pagar(id, "TX-1", criadoEm.plusSeconds(48 * 3600 + 1).toString()), 409, "PEDIDO_EXPIRADO");
        assertThat(situacao(id)).isEqualTo("CANCELADO");
        assertThat(estoque("CAM-001")).isEqualTo(50);
        assertThat(avisos(CPF)).containsExactly("PEDIDO_CANCELADO");
    }

    @Test
    void faltaDeEstoqueNoPagamentoCancelaOPedido() throws Exception {
        long primeiro = registrar(compra(CPF, "MOC-001", "1", "CAM-001", "1"));
        long segundo = registrar(compra("98765432100", "MOC-001", "1", "CAM-001", "1"));
        ok(pagarAgora(primeiro, "TX-1"));
        erro(pagarAgora(segundo, "TX-2"), 409, "ESTOQUE_INSUFICIENTE");
        assertThat(situacao(segundo)).isEqualTo("CANCELADO");
        assertThat(estoque("MOC-001")).isZero();
        assertThat(estoque("CAM-001")).isEqualTo(49);
        assertThat(avisos("98765432100")).containsExactly("PEDIDO_CANCELADO");
        // agora a mochila não pode mais nem ser colocada num pedido
        erro(postar("/pedidos", compra(CPF, "MOC-001", "1").json()), 422, "ESTOQUE_INSUFICIENTE");
    }

    @Test
    void pagamentosSimultaneosNuncaVendemMaisQueOEstoque() throws Exception {
        List<Long> pedidos = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            pedidos.add(registrar(compra(String.format("%011d", i), "JAQ-001", "1")));
        }
        List<Callable<Integer>> pagamentos = new ArrayList<>();
        for (long id : pedidos) {
            pagamentos.add(() -> pagarAgora(id, "TX-" + id).getResponse().getStatus());
            pagamentos.add(() -> pagarAgora(id, "TX-" + id).getResponse().getStatus()); // aviso repetido
        }
        ExecutorService executor = Executors.newFixedThreadPool(12);
        List<Integer> situacoes = new ArrayList<>();
        try {
            for (Future<Integer> resultado : executor.invokeAll(pagamentos)) {
                situacoes.add(resultado.get());
            }
        } finally {
            executor.shutdown();
        }
        assertThat(situacoes).containsOnly(200, 409);
        long pagos = 0;
        for (long id : pedidos) {
            if (situacao(id).equals("PAGO")) {
                pagos++;
            }
        }
        assertThat(pagos).isEqualTo(3);
        assertThat(estoque("JAQ-001")).isZero();
    }

    @Test
    void bemVindoSoNaPrimeiraCompra() throws Exception {
        Compra comCupom = compra(CPF, "CAM-001", "1").cupom("BEMVINDO10");
        long cancelado = registrar(comCupom);
        ok(pagarAgora(cancelado, "TX-1"));
        ok(postar("/pedidos/" + cancelado + "/cancelamento"));
        // pedido cancelado não conta
        long primeiro = registrar(comCupom);
        // pedido aguardando pagamento também não conta
        registrar(comCupom);
        ok(pagarAgora(primeiro, "TX-2"));
        erro(postar("/checkout/resumo", comCupom.json()), 422, "CUPOM_NAO_APLICAVEL");
        erro(postar("/pedidos", comCupom.json()), 422, "CUPOM_NAO_APLICAVEL");
    }

    @Test
    void avisosEmOrdemEMarcacaoDeLeitura() throws Exception {
        long id = registrar(compra(CPF, "CAM-001", "1"));
        ok(pagarAgora(id, "TX-1"));
        ok(postar("/pedidos/" + id + "/cancelamento"));
        MvcResult lista = consultar("/clientes/" + CPF + "/avisos");
        List<Integer> ids = ler(lista, "$[*].id");
        assertThat(ids).hasSize(2);
        assertThat(corpo(lista)).isEqualTo("["
                + "{\"id\":" + ids.get(0) + ",\"tipo\":\"PAGAMENTO_APROVADO\",\"pedidoId\":" + id + ",\"situacao\":\"NOVO\"},"
                + "{\"id\":" + ids.get(1) + ",\"tipo\":\"PEDIDO_CANCELADO\",\"pedidoId\":" + id + ",\"situacao\":\"NOVO\"}]");

        MvcResult lido = postar("/avisos/" + ids.get(0) + "/leitura");
        ok(lido);
        assertThat((String) ler(lido, "$.situacao")).isEqualTo("LIDO");
        ok(postar("/avisos/" + ids.get(0) + "/leitura"));
        assertThat((String) ler(postar("/avisos/" + ids.get(0) + "/leitura"), "$.situacao")).isEqualTo("LIDO");
        assertThat(ler(consultar("/clientes/" + CPF + "/avisos"), "$[*].situacao").toString()).isEqualTo("[\"LIDO\",\"NOVO\"]");
    }

    @Test
    void naoEncontrados() throws Exception {
        erro(consultar("/pedidos/999999"), 404, "PEDIDO_NAO_ENCONTRADO");
        erro(consultar("/pedidos/abc"), 404, "PEDIDO_NAO_ENCONTRADO");
        erro(pagar(999999, "TX-1", "2026-10-14T16:00:00Z"), 404, "PEDIDO_NAO_ENCONTRADO");
        erro(postar("/pedidos/999999/envio", "{\"codigoRastreio\": \"BR1\"}"), 404, "PEDIDO_NAO_ENCONTRADO");
        erro(postar("/pedidos/999999/entrega"), 404, "PEDIDO_NAO_ENCONTRADO");
        erro(postar("/pedidos/999999/cancelamento"), 404, "PEDIDO_NAO_ENCONTRADO");
        erro(consultar("/clientes/00000000000"), 404, "CLIENTE_NAO_ENCONTRADO");
        erro(consultar("/clientes/00000000000/avisos"), 404, "CLIENTE_NAO_ENCONTRADO");
        erro(postar("/avisos/999999/leitura"), 404, "AVISO_NAO_ENCONTRADO");
    }

    @Test
    void pedidoInvalidoNaoRegistraNada() throws Exception {
        erro(postar("/pedidos", compra(CPF, "CAM-001", "1").regiao("MARTE").json()), 422, "REGIAO_INVALIDA");
        erro(postar("/pedidos", "{ isto não é json"), 422, "PEDIDO_INVALIDO");
        erro(consultar("/clientes/" + CPF), 404, "CLIENTE_NAO_ENCONTRADO");
    }
}
