package com.loja.checkout.service;

import com.loja.checkout.dto.ItemRequest;
import com.loja.checkout.dto.ResumoRequest;
import com.loja.checkout.dto.ResumoResponse;
import com.loja.checkout.exception.CheckoutException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CheckoutServiceTest {

    private final CheckoutService service = new CheckoutService();

    private static List<ItemRequest> itensExemplo() {
        return List.of(
                new ItemRequest("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
                new ItemRequest("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"))
        );
    }

    @Test
    void exemplo5CompletoComClubeOuroERegiaoSudeste() {
        ResumoRequest request = new ResumoRequest(
                itensExemplo(), "EXPRESSA", null, "PIX", 1, "OURO", "SUDESTE");

        ResumoResponse resposta = service.calcularResumo(request);

        assertThat(resposta.subtotalProdutos()).isEqualByComparingTo("409.70");
        assertThat(resposta.descontoCupom()).isEqualByComparingTo("0.00");
        assertThat(resposta.frete()).isEqualByComparingTo("0.00");
        assertThat(resposta.prazoEntregaDias()).isEqualTo(2);
        assertThat(resposta.imposto()).isEqualByComparingTo("49.16");
        assertThat(resposta.ajustePagamento()).isEqualByComparingTo("-22.94");
        assertThat(resposta.totalFinal()).isEqualByComparingTo("435.92");
        assertThat(resposta.parcelas()).isEqualTo(1);
        assertThat(resposta.valorParcela()).isEqualByComparingTo("435.92");
        assertThat(resposta.creditoProximaCompra()).isEqualByComparingTo("20.48");
        assertThat(resposta.brinde()).isFalse();
    }

    @Test
    void exemplo1SubtotalCupomFreteEPagamento() {
        ResumoRequest request = new ResumoRequest(
                itensExemplo(), "EXPRESSA", "BEMVINDO10", "PIX", 1, "BRONZE", "NORTE");

        ResumoResponse resposta = service.calcularResumo(request);

        assertThat(resposta.subtotalProdutos()).isEqualByComparingTo("409.70");
        assertThat(resposta.descontoCupom()).isEqualByComparingTo("40.97");
        assertThat(resposta.frete()).isEqualByComparingTo("33.10");
        assertThat(resposta.prazoEntregaDias()).isEqualTo(2);
    }

    @Test
    void pedidoVazioRetornaPedidoInvalido() {
        ResumoRequest request = new ResumoRequest(
                List.of(), "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcularResumo(request))
                .isInstanceOf(CheckoutException.class)
                .extracting(e -> ((CheckoutException) e).getCodigo())
                .isEqualTo("PEDIDO_INVALIDO");
    }

    @Test
    void itemComQuantidadeZeroRetornaPedidoInvalido() {
        List<ItemRequest> itens = List.of(new ItemRequest("Camiseta", new BigDecimal("79.90"), 0, new BigDecimal("0.30")));
        ResumoRequest request = new ResumoRequest(
                itens, "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcularResumo(request))
                .isInstanceOf(CheckoutException.class)
                .extracting(e -> ((CheckoutException) e).getCodigo())
                .isEqualTo("PEDIDO_INVALIDO");
    }

    @Test
    void nivelClubeInvalidoTemPrioridadeSobreRegiaoEModalidade() {
        ResumoRequest request = new ResumoRequest(
                itensExemplo(), "MODALIDADE_QUE_NAO_EXISTE", null, "PIX", 1, "DIAMANTE", "REGIAO_QUE_NAO_EXISTE");

        assertThatThrownBy(() -> service.calcularResumo(request))
                .isInstanceOf(CheckoutException.class)
                .extracting(e -> ((CheckoutException) e).getCodigo())
                .isEqualTo("NIVEL_CLUBE_INVALIDO");
    }

    @Test
    void regiaoInvalidaTemPrioridadeSobreModalidade() {
        ResumoRequest request = new ResumoRequest(
                itensExemplo(), "MODALIDADE_QUE_NAO_EXISTE", null, "PIX", 1, "BRONZE", "REGIAO_QUE_NAO_EXISTE");

        assertThatThrownBy(() -> service.calcularResumo(request))
                .isInstanceOf(CheckoutException.class)
                .extracting(e -> ((CheckoutException) e).getCodigo())
                .isEqualTo("REGIAO_INVALIDA");
    }

    @Test
    void modalidadeInvalidaQuandoNaoExiste() {
        ResumoRequest request = new ResumoRequest(
                itensExemplo(), "SEDEX_ESPACIAL", null, "PIX", 1, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcularResumo(request))
                .isInstanceOf(CheckoutException.class)
                .extracting(e -> ((CheckoutException) e).getCodigo())
                .isEqualTo("MODALIDADE_INVALIDA");
    }

    @Test
    void motoboyIndisponivelAcimaDeCincoQuilos() {
        List<ItemRequest> itensPesados = List.of(
                new ItemRequest("Caixa", new BigDecimal("10.00"), 1, new BigDecimal("6.00")));
        ResumoRequest request = new ResumoRequest(
                itensPesados, "MOTOBOY", null, "PIX", 1, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcularResumo(request))
                .isInstanceOf(CheckoutException.class)
                .extracting(e -> ((CheckoutException) e).getCodigo())
                .isEqualTo("MODALIDADE_INDISPONIVEL");
    }

    @Test
    void cupomInvalidoQuandoNaoExiste() {
        ResumoRequest request = new ResumoRequest(
                itensExemplo(), "EXPRESSA", "CUPOM_FANTASMA", "PIX", 1, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcularResumo(request))
                .isInstanceOf(CheckoutException.class)
                .extracting(e -> ((CheckoutException) e).getCodigo())
                .isEqualTo("CUPOM_INVALIDO");
    }

    @Test
    void cupomNaoAplicavelQuandoAbaixoDoMinimo() {
        List<ItemRequest> itensBaratos = List.of(
                new ItemRequest("Meia", new BigDecimal("19.90"), 1, new BigDecimal("0.10")));
        ResumoRequest request = new ResumoRequest(
                itensBaratos, "RETIRADA_LOJA", "MENOS50", "PIX", 1, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcularResumo(request))
                .isInstanceOf(CheckoutException.class)
                .extracting(e -> ((CheckoutException) e).getCodigo())
                .isEqualTo("CUPOM_NAO_APLICAVEL");
    }

    @Test
    void formaPagamentoInvalidaQuandoNaoExiste() {
        ResumoRequest request = new ResumoRequest(
                itensExemplo(), "EXPRESSA", null, "CARTEIRA_DIGITAL", 1, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcularResumo(request))
                .isInstanceOf(CheckoutException.class)
                .extracting(e -> ((CheckoutException) e).getCodigo())
                .isEqualTo("FORMA_PAGAMENTO_INVALIDA");
    }

    @Test
    void parcelamentoInvalidoParaPixComMaisDeUmaParcela() {
        ResumoRequest request = new ResumoRequest(
                itensExemplo(), "EXPRESSA", null, "PIX", 2, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcularResumo(request))
                .isInstanceOf(CheckoutException.class)
                .extracting(e -> ((CheckoutException) e).getCodigo())
                .isEqualTo("PARCELAMENTO_INVALIDO");
    }

    @Test
    void parcelamentoInvalidoParaCartaoAcimaDeDozeVezes() {
        ResumoRequest request = new ResumoRequest(
                itensExemplo(), "EXPRESSA", null, "CARTAO", 13, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcularResumo(request))
                .isInstanceOf(CheckoutException.class)
                .extracting(e -> ((CheckoutException) e).getCodigo())
                .isEqualTo("PARCELAMENTO_INVALIDO");
    }

    @Test
    void formaPagamentoIndisponivelParaBoletoAcimaDeMilReais() {
        List<ItemRequest> itensCaros = List.of(
                new ItemRequest("Notebook", new BigDecimal("1500.00"), 1, new BigDecimal("2.00")));
        ResumoRequest request = new ResumoRequest(
                itensCaros, "RETIRADA_LOJA", null, "BOLETO", 1, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcularResumo(request))
                .isInstanceOf(CheckoutException.class)
                .extracting(e -> ((CheckoutException) e).getCodigo())
                .isEqualTo("FORMA_PAGAMENTO_INDISPONIVEL");
    }

    @Test
    void parcelasNaoInformadasAssumeUma() {
        ResumoRequest request = new ResumoRequest(
                itensExemplo(), "EXPRESSA", null, "PIX", null, "BRONZE", "SUDESTE");

        ResumoResponse resposta = service.calcularResumo(request);

        assertThat(resposta.parcelas()).isEqualTo(1);
    }
}
