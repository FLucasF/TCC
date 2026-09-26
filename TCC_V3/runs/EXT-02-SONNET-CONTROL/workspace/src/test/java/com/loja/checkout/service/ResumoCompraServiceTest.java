package com.loja.checkout.service;

import com.loja.checkout.api.dto.ItemRequest;
import com.loja.checkout.api.dto.ResumoRequest;
import com.loja.checkout.api.dto.ResumoResponse;
import com.loja.checkout.cupom.Bemvindo10Cupom;
import com.loja.checkout.cupom.CupomRegistry;
import com.loja.checkout.cupom.FreteGratisCupom;
import com.loja.checkout.cupom.Leve3Pague2Cupom;
import com.loja.checkout.cupom.Menos50Cupom;
import com.loja.checkout.entrega.EconomicaModalidade;
import com.loja.checkout.entrega.ExpressaModalidade;
import com.loja.checkout.entrega.ModalidadeEntregaRegistry;
import com.loja.checkout.entrega.MotoboyModalidade;
import com.loja.checkout.entrega.RetiradaLojaModalidade;
import com.loja.checkout.exception.CodigoErro;
import com.loja.checkout.exception.NegocioException;
import com.loja.checkout.pagamento.BoletoCalculadora;
import com.loja.checkout.pagamento.CalculadoraPagamentoRegistry;
import com.loja.checkout.pagamento.CartaoCalculadora;
import com.loja.checkout.pagamento.PixCalculadora;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResumoCompraServiceTest {

    private ResumoCompraService service;

    @BeforeEach
    void setUp() {
        ModalidadeEntregaRegistry modalidadeEntregaRegistry = new ModalidadeEntregaRegistry(List.of(
                new EconomicaModalidade(), new ExpressaModalidade(), new RetiradaLojaModalidade(), new MotoboyModalidade()));
        CupomRegistry cupomRegistry = new CupomRegistry(List.of(
                new Bemvindo10Cupom(), new Menos50Cupom(), new FreteGratisCupom(), new Leve3Pague2Cupom()));
        CalculadoraPagamentoRegistry calculadoraPagamentoRegistry = new CalculadoraPagamentoRegistry(List.of(
                new PixCalculadora(), new CartaoCalculadora(), new BoletoCalculadora()));

        service = new ResumoCompraService(modalidadeEntregaRegistry, cupomRegistry, calculadoraPagamentoRegistry);
    }

    private static List<ItemRequest> itensCamisetaETenis() {
        return List.of(
                new ItemRequest("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
                new ItemRequest("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"))
        );
    }

    @Test
    void exemplo5_ouroNaoPagaFreteEGanhaCredito() {
        ResumoRequest request = new ResumoRequest(
                itensCamisetaETenis(), "EXPRESSA", null, "PIX", 1, "OURO", "SUDESTE");

        ResumoResponse resposta = service.calcular(request);

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
    void exemplo1_bemvindo10ComExpressaEPix() {
        ResumoRequest request = new ResumoRequest(
                itensCamisetaETenis(), "EXPRESSA", "BEMVINDO10", "PIX", 1, "BRONZE", "NORTE");

        ResumoResponse resposta = service.calcular(request);

        assertThat(resposta.subtotalProdutos()).isEqualByComparingTo("409.70");
        assertThat(resposta.descontoCupom()).isEqualByComparingTo("40.97");
        assertThat(resposta.frete()).isEqualByComparingTo("33.10");
        assertThat(resposta.prazoEntregaDias()).isEqualTo(2);
    }

    @Test
    void carrinhoVazioRetornaPedidoInvalido() {
        ResumoRequest request = new ResumoRequest(List.of(), "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcular(request))
                .isInstanceOf(NegocioException.class)
                .extracting(e -> ((NegocioException) e).getCodigo())
                .isEqualTo(CodigoErro.PEDIDO_INVALIDO);
    }

    @Test
    void itemComQuantidadeZeroRetornaPedidoInvalido() {
        ResumoRequest request = new ResumoRequest(
                List.of(new ItemRequest("Camiseta", new BigDecimal("79.90"), 0, new BigDecimal("0.30"))),
                "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcular(request))
                .isInstanceOf(NegocioException.class)
                .extracting(e -> ((NegocioException) e).getCodigo())
                .isEqualTo(CodigoErro.PEDIDO_INVALIDO);
    }

    @Test
    void nivelClubeInvalido() {
        ResumoRequest request = new ResumoRequest(
                itensCamisetaETenis(), "EXPRESSA", null, "PIX", 1, "DIAMANTE", "SUDESTE");

        assertThatThrownBy(() -> service.calcular(request))
                .isInstanceOf(NegocioException.class)
                .extracting(e -> ((NegocioException) e).getCodigo())
                .isEqualTo(CodigoErro.NIVEL_CLUBE_INVALIDO);
    }

    @Test
    void regiaoInvalida() {
        ResumoRequest request = new ResumoRequest(
                itensCamisetaETenis(), "EXPRESSA", null, "PIX", 1, "BRONZE", "OESTE");

        assertThatThrownBy(() -> service.calcular(request))
                .isInstanceOf(NegocioException.class)
                .extracting(e -> ((NegocioException) e).getCodigo())
                .isEqualTo(CodigoErro.REGIAO_INVALIDA);
    }

    @Test
    void modalidadeInvalida() {
        ResumoRequest request = new ResumoRequest(
                itensCamisetaETenis(), "TELEPORTE", null, "PIX", 1, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcular(request))
                .isInstanceOf(NegocioException.class)
                .extracting(e -> ((NegocioException) e).getCodigo())
                .isEqualTo(CodigoErro.MODALIDADE_INVALIDA);
    }

    @Test
    void motoboyIndisponivelAcimaDeCincoQuilos() {
        ResumoRequest request = new ResumoRequest(
                List.of(new ItemRequest("Fardo", new BigDecimal("500.00"), 1, new BigDecimal("6.00"))),
                "MOTOBOY", null, "PIX", 1, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcular(request))
                .isInstanceOf(NegocioException.class)
                .extracting(e -> ((NegocioException) e).getCodigo())
                .isEqualTo(CodigoErro.MODALIDADE_INDISPONIVEL);
    }

    @Test
    void cupomInvalido() {
        ResumoRequest request = new ResumoRequest(
                itensCamisetaETenis(), "EXPRESSA", "NAOEXISTE", "PIX", 1, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcular(request))
                .isInstanceOf(NegocioException.class)
                .extracting(e -> ((NegocioException) e).getCodigo())
                .isEqualTo(CodigoErro.CUPOM_INVALIDO);
    }

    @Test
    void cupomNaoAplicavelAbaixoDoMinimo() {
        ResumoRequest request = new ResumoRequest(
                List.of(new ItemRequest("Meia", new BigDecimal("19.90"), 2, new BigDecimal("0.10"))),
                "RETIRADA_LOJA", "MENOS50", "PIX", 1, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcular(request))
                .isInstanceOf(NegocioException.class)
                .extracting(e -> ((NegocioException) e).getCodigo())
                .isEqualTo(CodigoErro.CUPOM_NAO_APLICAVEL);
    }

    @Test
    void formaPagamentoInvalida() {
        ResumoRequest request = new ResumoRequest(
                itensCamisetaETenis(), "EXPRESSA", null, "DINHEIRO", 1, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcular(request))
                .isInstanceOf(NegocioException.class)
                .extracting(e -> ((NegocioException) e).getCodigo())
                .isEqualTo(CodigoErro.FORMA_PAGAMENTO_INVALIDA);
    }

    @Test
    void parcelamentoInvalidoParaPix() {
        ResumoRequest request = new ResumoRequest(
                itensCamisetaETenis(), "EXPRESSA", null, "PIX", 2, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcular(request))
                .isInstanceOf(NegocioException.class)
                .extracting(e -> ((NegocioException) e).getCodigo())
                .isEqualTo(CodigoErro.PARCELAMENTO_INVALIDO);
    }

    @Test
    void parcelamentoInvalidoParaCartaoAcimaDeDoze() {
        ResumoRequest request = new ResumoRequest(
                itensCamisetaETenis(), "EXPRESSA", null, "CARTAO", 13, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcular(request))
                .isInstanceOf(NegocioException.class)
                .extracting(e -> ((NegocioException) e).getCodigo())
                .isEqualTo(CodigoErro.PARCELAMENTO_INVALIDO);
    }

    @Test
    void boletoIndisponivelAcimaDeMilReais() {
        ResumoRequest request = new ResumoRequest(
                List.of(new ItemRequest("Notebook", new BigDecimal("1200.00"), 1, new BigDecimal("2.00"))),
                "RETIRADA_LOJA", null, "BOLETO", 1, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcular(request))
                .isInstanceOf(NegocioException.class)
                .extracting(e -> ((NegocioException) e).getCodigo())
                .isEqualTo(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
    }

    @Test
    void parcelasNaoInformadasAssumeUma() {
        ResumoRequest request = new ResumoRequest(
                itensCamisetaETenis(), "EXPRESSA", null, "PIX", null, "BRONZE", "SUDESTE");

        ResumoResponse resposta = service.calcular(request);

        assertThat(resposta.parcelas()).isEqualTo(1);
    }
}
