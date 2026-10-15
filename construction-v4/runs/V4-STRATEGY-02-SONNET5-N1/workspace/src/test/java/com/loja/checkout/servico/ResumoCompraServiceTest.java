package com.loja.checkout.servico;

import com.loja.checkout.dominio.CodigoErro;
import com.loja.checkout.dominio.PedidoRecusadoException;
import com.loja.checkout.web.dto.ItemRequest;
import com.loja.checkout.web.dto.ResumoCompraRequest;
import com.loja.checkout.web.dto.ResumoCompraResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ResumoCompraServiceTest {

    private final ResumoCompraService service = new ResumoCompraService();

    private static final List<ItemRequest> ITENS_CAMISETA_TENIS = List.of(
            new ItemRequest("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
            new ItemRequest("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20")));

    @Test
    void exemplo1_expressaComBemVindo10EPixBronzeNorte() {
        ResumoCompraRequest requisicao = new ResumoCompraRequest(
                ITENS_CAMISETA_TENIS, "EXPRESSA", "BEMVINDO10", "PIX", null, "BRONZE", "NORTE");

        ResumoCompraResponse resposta = service.calcular(requisicao);

        assertThat(resposta.subtotalProdutos()).isEqualTo("409.70");
        assertThat(resposta.descontoCupom()).isEqualTo("40.97");
        assertThat(resposta.frete()).isEqualTo("33.10");
        assertThat(resposta.prazoEntregaDias()).isEqualTo(2);
        assertThat(resposta.seguro()).isEqualTo("10.24");
        assertThat(resposta.ajustePagamento()).isEqualTo("-20.60");
        assertThat(resposta.totalFinal()).isEqualTo("391.47");
        assertThat(resposta.parcelas()).isEqualTo(1);
        assertThat(resposta.valorParcela()).isEqualTo("391.47");
        assertThat(resposta.creditoProximaCompra()).isEqualTo("0.00");
        assertThat(resposta.brinde()).isFalse();
    }

    @Test
    void exemplo2_economicaSemCupomCartao6xPrataCentroOeste() {
        ResumoCompraRequest requisicao = new ResumoCompraRequest(
                ITENS_CAMISETA_TENIS, "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE");

        ResumoCompraResponse resposta = service.calcular(requisicao);

        assertThat(resposta.subtotalProdutos()).isEqualTo("409.70");
        assertThat(resposta.descontoCupom()).isEqualTo("0.00");
        assertThat(resposta.frete()).isEqualTo("15.60");
        assertThat(resposta.prazoEntregaDias()).isEqualTo(7);
        assertThat(resposta.seguro()).isEqualTo("6.15");
        assertThat(resposta.ajustePagamento()).isEqualTo("30.55");
        assertThat(resposta.totalFinal()).isEqualTo("462.00");
        assertThat(resposta.parcelas()).isEqualTo(6);
        assertThat(resposta.valorParcela()).isEqualTo("77.00");
        assertThat(resposta.creditoProximaCompra()).isEqualTo("8.19");
        assertThat(resposta.brinde()).isFalse();
    }

    @Test
    void exemplo3_motoboyComMenos50EBoletoBronzeNordeste() {
        List<ItemRequest> itens = List.of(
                new ItemRequest("Fone", new BigDecimal("199.90"), 2, new BigDecimal("0.25")));
        ResumoCompraRequest requisicao = new ResumoCompraRequest(
                itens, "MOTOBOY", "MENOS50", "BOLETO", null, "BRONZE", "NORDESTE");

        ResumoCompraResponse resposta = service.calcular(requisicao);

        assertThat(resposta.subtotalProdutos()).isEqualTo("399.80");
        assertThat(resposta.descontoCupom()).isEqualTo("50.00");
        assertThat(resposta.frete()).isEqualTo("18.00");
        assertThat(resposta.prazoEntregaDias()).isEqualTo(0);
        assertThat(resposta.seguro()).isEqualTo("8.00");
        assertThat(resposta.ajustePagamento()).isEqualTo("3.49");
        assertThat(resposta.totalFinal()).isEqualTo("379.29");
        assertThat(resposta.parcelas()).isEqualTo(1);
        assertThat(resposta.valorParcela()).isEqualTo("379.29");
        assertThat(resposta.creditoProximaCompra()).isEqualTo("0.00");
        assertThat(resposta.brinde()).isFalse();
    }

    @Test
    void exemplo4_retiradaLojaComLeve3Pague2ECartao3xPrataSul() {
        List<ItemRequest> itens = List.of(
                new ItemRequest("Meia", new BigDecimal("19.90"), 7, new BigDecimal("0.10")),
                new ItemRequest("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")));
        ResumoCompraRequest requisicao = new ResumoCompraRequest(
                itens, "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL");

        ResumoCompraResponse resposta = service.calcular(requisicao);

        assertThat(resposta.subtotalProdutos()).isEqualTo("299.10");
        assertThat(resposta.descontoCupom()).isEqualTo("39.80");
        assertThat(resposta.frete()).isEqualTo("0.00");
        assertThat(resposta.prazoEntregaDias()).isEqualTo(1);
        assertThat(resposta.seguro()).isEqualTo("2.99");
        assertThat(resposta.ajustePagamento()).isEqualTo("0.00");
        assertThat(resposta.totalFinal()).isEqualTo("262.29");
        assertThat(resposta.parcelas()).isEqualTo(3);
        assertThat(resposta.valorParcela()).isEqualTo("87.43");
        assertThat(resposta.creditoProximaCompra()).isEqualTo("5.98");
        assertThat(resposta.brinde()).isFalse();
    }

    @Test
    void exemplo5_expressaSemCupomPixOuroSudeste() {
        ResumoCompraRequest requisicao = new ResumoCompraRequest(
                ITENS_CAMISETA_TENIS, "EXPRESSA", null, "PIX", null, "OURO", "SUDESTE");

        ResumoCompraResponse resposta = service.calcular(requisicao);

        assertThat(resposta.subtotalProdutos()).isEqualTo("409.70");
        assertThat(resposta.descontoCupom()).isEqualTo("0.00");
        assertThat(resposta.frete()).isEqualTo("0.00");
        assertThat(resposta.prazoEntregaDias()).isEqualTo(2);
        assertThat(resposta.seguro()).isEqualTo("4.10");
        assertThat(resposta.ajustePagamento()).isEqualTo("-20.69");
        assertThat(resposta.totalFinal()).isEqualTo("393.11");
        assertThat(resposta.parcelas()).isEqualTo(1);
        assertThat(resposta.valorParcela()).isEqualTo("393.11");
        assertThat(resposta.creditoProximaCompra()).isEqualTo("20.48");
        assertThat(resposta.brinde()).isFalse();
    }

    @Test
    void carrinhoVazioEhPedidoInvalido() {
        ResumoCompraRequest requisicao = new ResumoCompraRequest(
                List.of(), "EXPRESSA", null, "PIX", null, "BRONZE", "SUDESTE");

        PedidoRecusadoException excecao = assertThrows(PedidoRecusadoException.class, () -> service.calcular(requisicao));

        assertThat(excecao.codigo()).isEqualTo(CodigoErro.PEDIDO_INVALIDO);
    }

    @Test
    void motoboyAcimaDe5KgEhModalidadeIndisponivel() {
        List<ItemRequest> itens = List.of(
                new ItemRequest("Caixa pesada", new BigDecimal("50.00"), 1, new BigDecimal("6.00")));
        ResumoCompraRequest requisicao = new ResumoCompraRequest(
                itens, "MOTOBOY", null, "PIX", null, "BRONZE", "SUDESTE");

        PedidoRecusadoException excecao = assertThrows(PedidoRecusadoException.class, () -> service.calcular(requisicao));

        assertThat(excecao.codigo()).isEqualTo(CodigoErro.MODALIDADE_INDISPONIVEL);
    }

    @Test
    void menos50AbaixoDoMinimoEhCupomNaoAplicavel() {
        List<ItemRequest> itens = List.of(
                new ItemRequest("Meia", new BigDecimal("19.90"), 1, new BigDecimal("0.10")));
        ResumoCompraRequest requisicao = new ResumoCompraRequest(
                itens, "RETIRADA_LOJA", "MENOS50", "PIX", null, "BRONZE", "SUDESTE");

        PedidoRecusadoException excecao = assertThrows(PedidoRecusadoException.class, () -> service.calcular(requisicao));

        assertThat(excecao.codigo()).isEqualTo(CodigoErro.CUPOM_NAO_APLICAVEL);
    }

    @Test
    void boletoAcimaDe1000EhFormaPagamentoIndisponivel() {
        List<ItemRequest> itens = List.of(
                new ItemRequest("Televisão", new BigDecimal("1200.00"), 1, new BigDecimal("8.00")));
        ResumoCompraRequest requisicao = new ResumoCompraRequest(
                itens, "RETIRADA_LOJA", null, "BOLETO", null, "BRONZE", "SUDESTE");

        PedidoRecusadoException excecao = assertThrows(PedidoRecusadoException.class, () -> service.calcular(requisicao));

        assertThat(excecao.codigo()).isEqualTo(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
    }

    @Test
    void cartaoCom13ParcelasEhParcelamentoInvalido() {
        List<ItemRequest> itens = List.of(
                new ItemRequest("Meia", new BigDecimal("19.90"), 1, new BigDecimal("0.10")));
        ResumoCompraRequest requisicao = new ResumoCompraRequest(
                itens, "RETIRADA_LOJA", null, "CARTAO", 13, "BRONZE", "SUDESTE");

        PedidoRecusadoException excecao = assertThrows(PedidoRecusadoException.class, () -> service.calcular(requisicao));

        assertThat(excecao.codigo()).isEqualTo(CodigoErro.PARCELAMENTO_INVALIDO);
    }
}
