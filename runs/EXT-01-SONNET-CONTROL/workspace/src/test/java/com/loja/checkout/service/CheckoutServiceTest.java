package com.loja.checkout.service;

import com.loja.checkout.dto.ItemPedidoRequest;
import com.loja.checkout.dto.ResumoRequest;
import com.loja.checkout.dto.ResumoResponse;
import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.exception.CodigoErro;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CheckoutServiceTest {

    private final CheckoutService service = new CheckoutService();

    private static final List<ItemPedidoRequest> ITENS_CAMISETA_TENIS = List.of(
            new ItemPedidoRequest("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
            new ItemPedidoRequest("Tenis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"))
    );

    // Exemplo 5 do enunciado: unico exemplo com regiao e nivel de clube informados,
    // portanto o unico que pode ser conferido ponta a ponta com os valores exatos do financeiro.
    @Test
    void exemplo5_ouroSudeste_pixSemCupom() {
        ResumoRequest request = new ResumoRequest(
                ITENS_CAMISETA_TENIS, "EXPRESSA", null, "PIX", 1, "OURO", "SUDESTE");

        ResumoResponse resumo = service.calcularResumo(request);

        assertThat(resumo.subtotalProdutos()).isEqualByComparingTo("409.70");
        assertThat(resumo.descontoCupom()).isEqualByComparingTo("0.00");
        assertThat(resumo.frete()).isEqualByComparingTo("0.00");
        assertThat(resumo.prazoEntregaDias()).isEqualTo(2);
        assertThat(resumo.imposto()).isEqualByComparingTo("49.16");
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo("-22.94");
        assertThat(resumo.totalFinal()).isEqualByComparingTo("435.92");
        assertThat(resumo.parcelas()).isEqualTo(1);
        assertThat(resumo.valorParcela()).isEqualByComparingTo("435.92");
        assertThat(resumo.creditoProximaCompra()).isEqualByComparingTo("20.48");
        assertThat(resumo.brinde()).isFalse();
    }

    // Os exemplos 1 a 4 do enunciado nao informam regiao/clube; os valores abaixo
    // foram recalculados com BRONZE + regiao para exercitar o fluxo completo (imposto e credito inclusos).
    @Test
    void exemplo1Estendido_expressaBemvindo10Pix() {
        ResumoRequest request = new ResumoRequest(
                ITENS_CAMISETA_TENIS, "EXPRESSA", "BEMVINDO10", "PIX", 1, "BRONZE", "SUDESTE");

        ResumoResponse resumo = service.calcularResumo(request);

        assertThat(resumo.subtotalProdutos()).isEqualByComparingTo("409.70");
        assertThat(resumo.descontoCupom()).isEqualByComparingTo("40.97");
        assertThat(resumo.frete()).isEqualByComparingTo("33.10");
        assertThat(resumo.prazoEntregaDias()).isEqualTo(2);
        assertThat(resumo.imposto()).isEqualByComparingTo("44.25");
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo("-22.30");
        assertThat(resumo.totalFinal()).isEqualByComparingTo("423.78");
        assertThat(resumo.creditoProximaCompra()).isEqualByComparingTo("0.00");
        assertThat(resumo.brinde()).isFalse();
    }

    @Test
    void exemplo2Estendido_economicaSemCupomCartao6x() {
        ResumoRequest request = new ResumoRequest(
                ITENS_CAMISETA_TENIS, "ECONOMICA", null, "CARTAO", 6, "BRONZE", "SUDESTE");

        ResumoResponse resumo = service.calcularResumo(request);

        assertThat(resumo.frete()).isEqualByComparingTo("15.60");
        assertThat(resumo.prazoEntregaDias()).isEqualTo(7);
        assertThat(resumo.imposto()).isEqualByComparingTo("49.16");
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo("33.56");
        assertThat(resumo.totalFinal()).isEqualByComparingTo("508.02");
        assertThat(resumo.parcelas()).isEqualTo(6);
        assertThat(resumo.valorParcela()).isEqualByComparingTo("84.67");
    }

    @Test
    void exemplo3Estendido_motoboyMenos50Boleto() {
        List<ItemPedidoRequest> itens = List.of(
                new ItemPedidoRequest("Fone", new BigDecimal("199.90"), 2, new BigDecimal("0.25")));
        ResumoRequest request = new ResumoRequest(
                itens, "MOTOBOY", "MENOS50", "BOLETO", 1, "BRONZE", "NORDESTE");

        ResumoResponse resumo = service.calcularResumo(request);

        assertThat(resumo.subtotalProdutos()).isEqualByComparingTo("399.80");
        assertThat(resumo.descontoCupom()).isEqualByComparingTo("50.00");
        assertThat(resumo.frete()).isEqualByComparingTo("18.00");
        assertThat(resumo.prazoEntregaDias()).isEqualTo(0);
        assertThat(resumo.imposto()).isEqualByComparingTo("24.49");
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo("3.49");
        assertThat(resumo.totalFinal()).isEqualByComparingTo("395.78");
    }

    @Test
    void exemplo4Estendido_retiradaLoja_leve3pague2_cartao3x() {
        List<ItemPedidoRequest> itens = List.of(
                new ItemPedidoRequest("Meia", new BigDecimal("19.90"), 7, new BigDecimal("0.10")),
                new ItemPedidoRequest("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30"))
        );
        ResumoRequest request = new ResumoRequest(
                itens, "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "BRONZE", "NORDESTE");

        ResumoResponse resumo = service.calcularResumo(request);

        assertThat(resumo.subtotalProdutos()).isEqualByComparingTo("299.10");
        assertThat(resumo.descontoCupom()).isEqualByComparingTo("39.80");
        assertThat(resumo.frete()).isEqualByComparingTo("0.00");
        assertThat(resumo.prazoEntregaDias()).isEqualTo(1);
        assertThat(resumo.imposto()).isEqualByComparingTo("18.15");
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo("0.00");
        assertThat(resumo.totalFinal()).isEqualByComparingTo("277.45");
        assertThat(resumo.valorParcela()).isEqualByComparingTo("92.48");
    }

    @Test
    void carrinhoVazioDaPedidoInvalido() {
        ResumoRequest request = new ResumoRequest(
                List.of(), "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcularResumo(request))
                .isInstanceOf(CheckoutException.class)
                .extracting(ex -> ((CheckoutException) ex).getCodigo())
                .isEqualTo(CodigoErro.PEDIDO_INVALIDO);
    }

    @Test
    void itemComPrecoZeroDaPedidoInvalido() {
        List<ItemPedidoRequest> itens = List.of(
                new ItemPedidoRequest("Camiseta", BigDecimal.ZERO, 1, new BigDecimal("0.3")));
        ResumoRequest request = new ResumoRequest(
                itens, "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcularResumo(request))
                .isInstanceOf(CheckoutException.class)
                .extracting(ex -> ((CheckoutException) ex).getCodigo())
                .isEqualTo(CodigoErro.PEDIDO_INVALIDO);
    }

    @Test
    void nivelClubeInvalidoOuAusente() {
        ResumoRequest request = new ResumoRequest(
                ITENS_CAMISETA_TENIS, "EXPRESSA", null, "PIX", 1, "DIAMANTE", "SUDESTE");

        assertThatThrownBy(() -> service.calcularResumo(request))
                .isInstanceOf(CheckoutException.class)
                .extracting(ex -> ((CheckoutException) ex).getCodigo())
                .isEqualTo(CodigoErro.NIVEL_CLUBE_INVALIDO);
    }

    @Test
    void regiaoInvalidaOuAusente() {
        ResumoRequest request = new ResumoRequest(
                ITENS_CAMISETA_TENIS, "EXPRESSA", null, "PIX", 1, "BRONZE", null);

        assertThatThrownBy(() -> service.calcularResumo(request))
                .isInstanceOf(CheckoutException.class)
                .extracting(ex -> ((CheckoutException) ex).getCodigo())
                .isEqualTo(CodigoErro.REGIAO_INVALIDA);
    }

    @Test
    void modalidadeInvalidaOuAusente() {
        ResumoRequest request = new ResumoRequest(
                ITENS_CAMISETA_TENIS, "TELETRANSPORTE", null, "PIX", 1, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcularResumo(request))
                .isInstanceOf(CheckoutException.class)
                .extracting(ex -> ((CheckoutException) ex).getCodigo())
                .isEqualTo(CodigoErro.MODALIDADE_INVALIDA);
    }

    @Test
    void motoboyIndisponivelAcimaDeCincoQuilos() {
        List<ItemPedidoRequest> itens = List.of(
                new ItemPedidoRequest("Halter", new BigDecimal("100.00"), 1, new BigDecimal("6")));
        ResumoRequest request = new ResumoRequest(
                itens, "MOTOBOY", null, "PIX", 1, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcularResumo(request))
                .isInstanceOf(CheckoutException.class)
                .extracting(ex -> ((CheckoutException) ex).getCodigo())
                .isEqualTo(CodigoErro.MODALIDADE_INDISPONIVEL);
    }

    @Test
    void cupomInvalido() {
        ResumoRequest request = new ResumoRequest(
                ITENS_CAMISETA_TENIS, "EXPRESSA", "NAOEXISTE", "PIX", 1, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcularResumo(request))
                .isInstanceOf(CheckoutException.class)
                .extracting(ex -> ((CheckoutException) ex).getCodigo())
                .isEqualTo(CodigoErro.CUPOM_INVALIDO);
    }

    @Test
    void cupomNaoAplicavelQuandoAbaixoDoMinimo() {
        List<ItemPedidoRequest> itens = List.of(
                new ItemPedidoRequest("Meia", new BigDecimal("19.90"), 1, new BigDecimal("0.1")));
        ResumoRequest request = new ResumoRequest(
                itens, "RETIRADA_LOJA", "MENOS50", "PIX", 1, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcularResumo(request))
                .isInstanceOf(CheckoutException.class)
                .extracting(ex -> ((CheckoutException) ex).getCodigo())
                .isEqualTo(CodigoErro.CUPOM_NAO_APLICAVEL);
    }

    @Test
    void formaPagamentoInvalidaOuAusente() {
        ResumoRequest request = new ResumoRequest(
                ITENS_CAMISETA_TENIS, "EXPRESSA", null, "CRIPTOMOEDA", 1, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcularResumo(request))
                .isInstanceOf(CheckoutException.class)
                .extracting(ex -> ((CheckoutException) ex).getCodigo())
                .isEqualTo(CodigoErro.FORMA_PAGAMENTO_INVALIDA);
    }

    @Test
    void parcelamentoInvalidoParaPix() {
        ResumoRequest request = new ResumoRequest(
                ITENS_CAMISETA_TENIS, "EXPRESSA", null, "PIX", 2, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcularResumo(request))
                .isInstanceOf(CheckoutException.class)
                .extracting(ex -> ((CheckoutException) ex).getCodigo())
                .isEqualTo(CodigoErro.PARCELAMENTO_INVALIDO);
    }

    @Test
    void parcelamentoInvalidoParaCartaoAcimaDeDoze() {
        ResumoRequest request = new ResumoRequest(
                ITENS_CAMISETA_TENIS, "EXPRESSA", null, "CARTAO", 13, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcularResumo(request))
                .isInstanceOf(CheckoutException.class)
                .extracting(ex -> ((CheckoutException) ex).getCodigo())
                .isEqualTo(CodigoErro.PARCELAMENTO_INVALIDO);
    }

    @Test
    void boletoIndisponivelAcimaDeMilReais() {
        List<ItemPedidoRequest> itens = List.of(
                new ItemPedidoRequest("Notebook", new BigDecimal("2000.00"), 1, new BigDecimal("2")));
        ResumoRequest request = new ResumoRequest(
                itens, "RETIRADA_LOJA", null, "BOLETO", 1, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcularResumo(request))
                .isInstanceOf(CheckoutException.class)
                .extracting(ex -> ((CheckoutException) ex).getCodigo())
                .isEqualTo(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
    }

    @Test
    void ouroComPedidoAcimaDeQuinhentosGanhaBrinde() {
        List<ItemPedidoRequest> itens = List.of(
                new ItemPedidoRequest("Tenis", new BigDecimal("249.90"), 3, new BigDecimal("1.2")));
        ResumoRequest request = new ResumoRequest(
                itens, "RETIRADA_LOJA", null, "PIX", 1, "OURO", "SUDESTE");

        ResumoResponse resumo = service.calcularResumo(request);

        assertThat(resumo.brinde()).isTrue();
    }
}
