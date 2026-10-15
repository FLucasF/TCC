package br.com.loja.pedidos;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

class CheckoutTest extends TesteIntegracao {

    private MvcResult resumo(Compra compra) throws Exception {
        return postar("/checkout/resumo", compra.json());
    }

    private static void valores(MvcResult resultado, String... campoEValor) throws Exception {
        assertThat(resultado.getResponse().getStatus()).as(corpo(resultado)).isEqualTo(200);
        String json = corpo(resultado);
        for (int i = 0; i < campoEValor.length; i += 2) {
            assertThat(json).contains("\"" + campoEValor[i] + "\":" + campoEValor[i + 1]);
        }
    }

    private static void erro(MvcResult resultado, int status, String codigo) throws Exception {
        assertThat(corpo(resultado)).isEqualTo("{\"erro\":\"" + codigo + "\"}");
        assertThat(resultado.getResponse().getStatus()).isEqualTo(status);
    }

    @Test
    void retiradaNaLojaComPix() throws Exception {
        valores(resumo(compra("11111111111", "CAM-001", "2")),
                "subtotalProdutos", "159.80",
                "descontoCupom", "0.00",
                "frete", "0.00",
                "prazoEntregaDias", "1",
                "seguro", "1.60",
                "ajustePagamento", "-8.07",
                "totalFinal", "153.33",
                "parcelas", "1",
                "valorParcela", "153.33",
                "creditoProximaCompra", "0.00",
                "brinde", "false");
    }

    @Test
    void expressaPelaTransLogComDiasAMaisNoNorte() throws Exception {
        MvcResult resultado = resumo(compra("11111111111", "TEN-001", "1", "CAM-001", "2")
                .entrega("EXPRESSA").regiao("NORTE").pagamento("BOLETO"));
        // produtos 409.70; frete 33.10; seguro 2,5% = 10.2425 -> 10.24; total 453.04; boleto +3.49
        valores(resultado,
                "subtotalProdutos", "409.70",
                "frete", "33.10",
                "prazoEntregaDias", "5",
                "seguro", "10.24",
                "ajustePagamento", "3.49",
                "totalFinal", "456.53");
        assertThat(CONSULTAS_TRANSLOG).singleElement()
                .satisfies(uri -> assertThat(uri.toString()).isEqualTo("/v2/quote?zip=01310100&weight_g=1800&service=EXP"));
    }

    @Test
    void economicaUsaServicoStdENordesteTemDoisDiasAMais() throws Exception {
        transLog = uri -> RespostaTransLog.cotacao(1500, 48);
        valores(resumo(compra("11111111111", "MEI-001", "3").entrega("ECONOMICA").regiao("NORDESTE")),
                "frete", "15.00", "prazoEntregaDias", "4");
        assertThat(CONSULTAS_TRANSLOG.getFirst().getQuery()).isEqualTo("zip=01310100&weight_g=300&service=STD");
    }

    @Test
    void seguroTemLimiteNoNorteENordeste() throws Exception {
        valores(resumo(compra("11111111111", "JAQ-001", "3", "TEN-001", "3").regiao("NORDESTE").pagamento("CARTAO")),
                "subtotalProdutos", "1919.40", "seguro", "30.00");
        valores(resumo(compra("11111111111", "JAQ-001", "3").regiao("NORTE").pagamento("CARTAO")),
                "seguro", "29.24");
        valores(resumo(compra("11111111111", "JAQ-001", "3").regiao("CENTRO_OESTE").pagamento("CARTAO")),
                "seguro", "17.55");
    }

    @Test
    void arredondamentoMeioParaOPar() throws Exception {
        // 15 meias = 298.50; seguro de 1% = 2.985 -> 2.98
        valores(resumo(compra("11111111111", "MEI-001", "15")), "subtotalProdutos", "298.50", "seguro", "2.98");
    }

    @Test
    void cartaoAteTresVezesSemJuros() throws Exception {
        // 159.80 + 1.60 = 161.40 / 3 = 53.80
        valores(resumo(compra("11111111111", "CAM-001", "2").pagamento("CARTAO").parcelas(3)),
                "ajustePagamento", "0.00", "totalFinal", "161.40", "parcelas", "3", "valorParcela", "53.80");
    }

    @Test
    void cartaoComJurosPelaTabelaPrice() throws Exception {
        // 779.80 + 7.80 de seguro = 787.60
        double total = 787.60;
        double parcela = Math.round(total * 0.0199 / (1 - Math.pow(1.0199, -6)) * 100) / 100.0;
        assertThat(parcela).isEqualTo(140.56);
        valores(resumo(compra("11111111111", "JAQ-001", "2").pagamento("CARTAO").parcelas(6)),
                "valorParcela", "140.56", "totalFinal", "843.36", "ajustePagamento", "55.76", "parcelas", "6");
    }

    @Test
    void ouroNaoPagaFreteGanhaCreditoEBrinde() throws Exception {
        valores(resumo(compra("11111111111", "JAQ-001", "2").entrega("EXPRESSA").nivel("OURO").pagamento("CARTAO")),
                "frete", "0.00", "creditoProximaCompra", "38.99", "brinde", "true", "prazoEntregaDias", "2");
    }

    @Test
    void ouroPagaOMotoboy() throws Exception {
        valores(resumo(compra("11111111111", "CAM-001", "1").entrega("MOTOBOY").nivel("OURO")),
                "frete", "18.00", "prazoEntregaDias", "0", "brinde", "false", "creditoProximaCompra", "4.00");
    }

    @Test
    void prataGanhaDoisPorCento() throws Exception {
        valores(resumo(compra("11111111111", "CAM-001", "2").nivel("PRATA")), "creditoProximaCompra", "3.20");
    }

    @Test
    void motoboyAteCincoQuilos() throws Exception {
        // 50 meias = 5,00 kg: atende
        valores(resumo(compra("11111111111", "MEI-001", "50").entrega("MOTOBOY").pagamento("CARTAO")), "frete", "18.00");
        erro(resumo(compra("11111111111", "MEI-001", "51").entrega("MOTOBOY").pagamento("CARTAO")), 422, "MODALIDADE_INDISPONIVEL");
    }

    @Test
    void cupomBemVindoNaPrimeiraCompra() throws Exception {
        // 10% de 159.80 = 15.98; Pix sem desconto porque usou cupom
        valores(resumo(compra("11111111111", "CAM-001", "2").cupom("BEMVINDO10")),
                "descontoCupom", "15.98", "ajustePagamento", "0.00", "totalFinal", "145.42");
    }

    @Test
    void cupomMenos50SoAPartirDe300() throws Exception {
        erro(resumo(compra("11111111111", "TEN-001", "1").cupom("MENOS50")), 422, "CUPOM_NAO_APLICAVEL");
        valores(resumo(compra("11111111111", "TEN-001", "1", "MEI-001", "3").cupom("MENOS50")),
                "subtotalProdutos", "309.60", "descontoCupom", "50.00");
    }

    @Test
    void cupomFreteGratisDescontaOFrete() throws Exception {
        valores(resumo(compra("11111111111", "CAM-001", "2").entrega("EXPRESSA").cupom("FRETEGRATIS")),
                "frete", "33.10", "descontoCupom", "33.10");
        erro(resumo(compra("11111111111", "CAM-001", "2").cupom("FRETEGRATIS")), 422, "CUPOM_NAO_APLICAVEL");
    }

    @Test
    void cupomFreteGratisTambemRespeitaOLimiteDe30PorCento() throws Exception {
        // 30% de 19.90 = 5.97
        valores(resumo(compra("11111111111", "MEI-001", "1").entrega("MOTOBOY").cupom("FRETEGRATIS")),
                "frete", "18.00", "descontoCupom", "5.97");
    }

    @Test
    void cupomLeve3Pague2ComLimiteDe30PorCento() throws Exception {
        // 3 camisetas: 1 grátis = 79.90, mas o limite é 30% de 239.70 = 71.91
        valores(resumo(compra("11111111111", "CAM-001", "3").cupom("LEVE3PAGUE2")), "descontoCupom", "71.91");
        // 6 meias + 1 tênis: 2 meias grátis = 39.80
        valores(resumo(compra("11111111111", "MEI-001", "6", "TEN-001", "1").cupom("LEVE3PAGUE2")), "descontoCupom", "39.80");
        erro(resumo(compra("11111111111", "MEI-001", "2").cupom("LEVE3PAGUE2")), 422, "CUPOM_NAO_APLICAVEL");
    }

    @Test
    void cupomEmMinusculasNaoExiste() throws Exception {
        erro(resumo(compra("11111111111", "CAM-001", "1").cupom("bemvindo10")), 422, "CUPOM_INVALIDO");
    }

    @Test
    void boletoAteMilReaisEPixEBoletoSoAVista() throws Exception {
        erro(resumo(compra("11111111111", "JAQ-001", "3").pagamento("BOLETO")), 422, "FORMA_PAGAMENTO_INDISPONIVEL");
        erro(resumo(compra("11111111111", "CAM-001", "1").pagamento("PIX").parcelas(2)), 422, "PARCELAMENTO_INVALIDO");
        erro(resumo(compra("11111111111", "CAM-001", "1").pagamento("CARTAO").parcelas(13)), 422, "PARCELAMENTO_INVALIDO");
        erro(resumo(compra("11111111111", "CAM-001", "1").pagamento("CARTAO").parcelas(0)), 422, "PARCELAMENTO_INVALIDO");
        erro(resumo(compra("11111111111", "CAM-001", "1").pagamento("DINHEIRO")), 422, "FORMA_PAGAMENTO_INVALIDA");
    }

    @Test
    void conferenciasNaOrdemCombinada() throws Exception {
        erro(postar("/checkout/resumo", "{}"), 422, "PEDIDO_INVALIDO");
        erro(resumo(compra("11111111111", "CAM-001", "0")), 422, "PEDIDO_INVALIDO");
        erro(resumo(compra("11111111111", "CAM-001", "1", "CAM-001", "1")), 422, "PEDIDO_INVALIDO");
        erro(resumo(compra("", "CAM-001", "1")), 422, "PEDIDO_INVALIDO");
        erro(resumo(compra("11111111111", "REL-001", "1", "XXX-999", "1").nivel("X")), 422, "PRODUTO_INEXISTENTE");
        erro(resumo(compra("11111111111", "REL-001", "1", "BON-001", "1")), 422, "PRODUTO_FORA_DE_LINHA");
        erro(resumo(compra("11111111111", "BON-001", "1").nivel("X")), 422, "ESTOQUE_INSUFICIENTE");
        erro(resumo(compra("11111111111", "CAM-001", "1").nivel("DIAMANTE").regiao("X")), 422, "NIVEL_CLUBE_INVALIDO");
        erro(resumo(compra("11111111111", "CAM-001", "1").nivel(null)), 422, "NIVEL_CLUBE_INVALIDO");
        erro(resumo(compra("11111111111", "CAM-001", "1").regiao("LESTE").cep("1")), 422, "REGIAO_INVALIDA");
        erro(resumo(compra("11111111111", "CAM-001", "1").cep("01310-100").entrega("X")), 422, "CEP_INVALIDO");
        erro(resumo(compra("11111111111", "CAM-001", "1").cep(null)), 422, "CEP_INVALIDO");
        erro(resumo(compra("11111111111", "CAM-001", "1").entrega("DRONE").cupom("X")), 422, "MODALIDADE_INVALIDA");
        erro(resumo(compra("11111111111", "CAM-001", "1").cupom("X").pagamento("X")), 422, "CUPOM_INVALIDO");
    }

    @Test
    void transLogNaoAtendeOCep() throws Exception {
        transLog = uri -> new RespostaTransLog(200, "{\"error\": \"ZIP_NOT_SERVED\"}", 0);
        erro(resumo(compra("11111111111", "CAM-001", "1").entrega("ECONOMICA")), 422, "MODALIDADE_INDISPONIVEL");
    }

    @Test
    void transLogForaDoAr() throws Exception {
        for (Function<java.net.URI, RespostaTransLog> falha : java.util.List.<Function<java.net.URI, RespostaTransLog>>of(
                uri -> new RespostaTransLog(500, "{}", 0),
                uri -> new RespostaTransLog(200, "{\"error\": \"INTERNAL\"}", 0),
                uri -> new RespostaTransLog(200, "isto não é json", 0),
                uri -> new RespostaTransLog(200, "{\"price_cents\": 100}", 0),
                uri -> new RespostaTransLog(200, "{\"price_cents\": 100, \"eta_hours\": 5}", 1500))) {
            transLog = falha;
            erro(resumo(compra("11111111111", "CAM-001", "1").entrega("EXPRESSA").cupom("NAOEXISTE")), 503, "TRANSPORTADORA_INDISPONIVEL");
        }
    }

    @Test
    void resumoNaoRegistraNada() throws Exception {
        resumo(compra("22222222222", "CAM-001", "1"));
        erro(consultar("/clientes/22222222222"), 404, "CLIENTE_NAO_ENCONTRADO");
        assertThat(estoque("CAM-001")).isEqualTo(50);
    }

    @Test
    void consultaDeProduto() throws Exception {
        MvcResult resultado = consultar("/produtos/REL-001");
        assertThat(corpo(resultado)).isEqualTo(
                "{\"sku\":\"REL-001\",\"nome\":\"Relógio digital\",\"preco\":129.90,\"pesoKg\":0.20,\"estoque\":8,\"foraDeLinha\":true}");
        erro(consultar("/produtos/NAO-001"), 404, "PRODUTO_NAO_ENCONTRADO");
    }
}
