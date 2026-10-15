package com.loja.checkout.service;

import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.exception.CodigoErro;
import com.loja.checkout.web.dto.ItemRequest;
import com.loja.checkout.web.dto.ResumoRequest;
import com.loja.checkout.web.dto.ResumoResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResumoServiceTest {

    private final ResumoService service = new ResumoService();

    private static ItemRequest camiseta() {
        return new ItemRequest("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30"));
    }

    private static ItemRequest tenis() {
        return new ItemRequest("Tenis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"));
    }

    @Test
    void exemplo1_expressaBemvindo10PixBronzeNorte() {
        ResumoRequest req = new ResumoRequest(
                List.of(camiseta(), tenis()),
                "EXPRESSA", "BEMVINDO10", "PIX", null, "BRONZE", "NORTE");

        ResumoResponse r = service.calcular(req);

        assertThat(r.subtotalProdutos()).isEqualByComparingTo("409.70");
        assertThat(r.descontoCupom()).isEqualByComparingTo("40.97");
        assertThat(r.frete()).isEqualByComparingTo("33.10");
        assertThat(r.prazoEntregaDias()).isEqualTo(2);
        assertThat(r.seguro()).isEqualByComparingTo("10.24");
        assertThat(r.ajustePagamento()).isEqualByComparingTo("-20.60");
        assertThat(r.totalFinal()).isEqualByComparingTo("391.47");
        assertThat(r.parcelas()).isEqualTo(1);
        assertThat(r.valorParcela()).isEqualByComparingTo("391.47");
        assertThat(r.creditoProximaCompra()).isEqualByComparingTo("0.00");
        assertThat(r.brinde()).isFalse();
    }

    @Test
    void exemplo2_economicaSemCupomCartao6xPrataCentroOeste() {
        ResumoRequest req = new ResumoRequest(
                List.of(camiseta(), tenis()),
                "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE");

        ResumoResponse r = service.calcular(req);

        assertThat(r.subtotalProdutos()).isEqualByComparingTo("409.70");
        assertThat(r.descontoCupom()).isEqualByComparingTo("0.00");
        assertThat(r.frete()).isEqualByComparingTo("15.60");
        assertThat(r.prazoEntregaDias()).isEqualTo(7);
        assertThat(r.seguro()).isEqualByComparingTo("6.15");
        assertThat(r.ajustePagamento()).isEqualByComparingTo("30.55");
        assertThat(r.totalFinal()).isEqualByComparingTo("462.00");
        assertThat(r.parcelas()).isEqualTo(6);
        assertThat(r.valorParcela()).isEqualByComparingTo("77.00");
        assertThat(r.creditoProximaCompra()).isEqualByComparingTo("8.19");
        assertThat(r.brinde()).isFalse();
    }

    @Test
    void exemplo3_motoboyMenos50BoletoBronzeNordeste() {
        ItemRequest fone = new ItemRequest("Fone", new BigDecimal("199.90"), 2, new BigDecimal("0.25"));
        ResumoRequest req = new ResumoRequest(
                List.of(fone),
                "MOTOBOY", "MENOS50", "BOLETO", null, "BRONZE", "NORDESTE");

        ResumoResponse r = service.calcular(req);

        assertThat(r.subtotalProdutos()).isEqualByComparingTo("399.80");
        assertThat(r.descontoCupom()).isEqualByComparingTo("50.00");
        assertThat(r.frete()).isEqualByComparingTo("18.00");
        assertThat(r.prazoEntregaDias()).isEqualTo(0);
        assertThat(r.seguro()).isEqualByComparingTo("8.00");
        assertThat(r.ajustePagamento()).isEqualByComparingTo("3.49");
        assertThat(r.totalFinal()).isEqualByComparingTo("379.29");
        assertThat(r.parcelas()).isEqualTo(1);
        assertThat(r.valorParcela()).isEqualByComparingTo("379.29");
        assertThat(r.creditoProximaCompra()).isEqualByComparingTo("0.00");
        assertThat(r.brinde()).isFalse();
    }

    @Test
    void exemplo4_retiradaLoja3pague2Cartao3xPrataSul() {
        ItemRequest meia = new ItemRequest("Meia", new BigDecimal("19.90"), 7, new BigDecimal("0.10"));
        ResumoRequest req = new ResumoRequest(
                List.of(meia, camiseta()),
                "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL");

        ResumoResponse r = service.calcular(req);

        assertThat(r.subtotalProdutos()).isEqualByComparingTo("299.10");
        assertThat(r.descontoCupom()).isEqualByComparingTo("39.80");
        assertThat(r.frete()).isEqualByComparingTo("0.00");
        assertThat(r.prazoEntregaDias()).isEqualTo(1);
        assertThat(r.seguro()).isEqualByComparingTo("2.99");
        assertThat(r.ajustePagamento()).isEqualByComparingTo("0.00");
        assertThat(r.totalFinal()).isEqualByComparingTo("262.29");
        assertThat(r.parcelas()).isEqualTo(3);
        assertThat(r.valorParcela()).isEqualByComparingTo("87.43");
        assertThat(r.creditoProximaCompra()).isEqualByComparingTo("5.98");
        assertThat(r.brinde()).isFalse();
    }

    @Test
    void exemplo5_expressaSemCupomPixOuroSudeste() {
        ResumoRequest req = new ResumoRequest(
                List.of(camiseta(), tenis()),
                "EXPRESSA", null, "PIX", null, "OURO", "SUDESTE");

        ResumoResponse r = service.calcular(req);

        assertThat(r.subtotalProdutos()).isEqualByComparingTo("409.70");
        assertThat(r.descontoCupom()).isEqualByComparingTo("0.00");
        assertThat(r.frete()).isEqualByComparingTo("0.00");
        assertThat(r.prazoEntregaDias()).isEqualTo(2);
        assertThat(r.seguro()).isEqualByComparingTo("4.10");
        assertThat(r.ajustePagamento()).isEqualByComparingTo("-20.69");
        assertThat(r.totalFinal()).isEqualByComparingTo("393.11");
        assertThat(r.parcelas()).isEqualTo(1);
        assertThat(r.valorParcela()).isEqualByComparingTo("393.11");
        assertThat(r.creditoProximaCompra()).isEqualByComparingTo("20.48");
        assertThat(r.brinde()).isFalse();
    }

    @Test
    void carrinhoVazioDaPedidoInvalido() {
        ResumoRequest req = new ResumoRequest(
                List.of(), "ECONOMICA", null, "PIX", null, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcular(req))
                .isInstanceOf(CheckoutException.class)
                .extracting(e -> ((CheckoutException) e).getCodigo())
                .isEqualTo(CodigoErro.PEDIDO_INVALIDO);
    }

    @Test
    void itemComPrecoZeroDaPedidoInvalido() {
        ItemRequest item = new ItemRequest("Camiseta", BigDecimal.ZERO, 1, new BigDecimal("0.30"));
        ResumoRequest req = new ResumoRequest(
                List.of(item), "ECONOMICA", null, "PIX", null, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcular(req))
                .isInstanceOf(CheckoutException.class)
                .extracting(e -> ((CheckoutException) e).getCodigo())
                .isEqualTo(CodigoErro.PEDIDO_INVALIDO);
    }

    @Test
    void nivelClubeInvalidoEhDetectado() {
        ResumoRequest req = new ResumoRequest(
                List.of(camiseta()), "ECONOMICA", null, "PIX", null, "DIAMANTE", "SUDESTE");

        assertThatThrownBy(() -> service.calcular(req))
                .isInstanceOf(CheckoutException.class)
                .extracting(e -> ((CheckoutException) e).getCodigo())
                .isEqualTo(CodigoErro.NIVEL_CLUBE_INVALIDO);
    }

    @Test
    void regiaoInvalidaEhDetectada() {
        ResumoRequest req = new ResumoRequest(
                List.of(camiseta()), "ECONOMICA", null, "PIX", null, "BRONZE", "LUA");

        assertThatThrownBy(() -> service.calcular(req))
                .isInstanceOf(CheckoutException.class)
                .extracting(e -> ((CheckoutException) e).getCodigo())
                .isEqualTo(CodigoErro.REGIAO_INVALIDA);
    }

    @Test
    void modalidadeInvalidaEhDetectada() {
        ResumoRequest req = new ResumoRequest(
                List.of(camiseta()), "TELEPORTE", null, "PIX", null, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcular(req))
                .isInstanceOf(CheckoutException.class)
                .extracting(e -> ((CheckoutException) e).getCodigo())
                .isEqualTo(CodigoErro.MODALIDADE_INVALIDA);
    }

    @Test
    void motoboyAcimaDe5kgEhIndisponivel() {
        ItemRequest pesado = new ItemRequest("Sofa", new BigDecimal("500.00"), 1, new BigDecimal("6.00"));
        ResumoRequest req = new ResumoRequest(
                List.of(pesado), "MOTOBOY", null, "PIX", null, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcular(req))
                .isInstanceOf(CheckoutException.class)
                .extracting(e -> ((CheckoutException) e).getCodigo())
                .isEqualTo(CodigoErro.MODALIDADE_INDISPONIVEL);
    }

    @Test
    void cupomInvalidoEhDetectado() {
        ResumoRequest req = new ResumoRequest(
                List.of(camiseta()), "ECONOMICA", "NAOEXISTE", "PIX", null, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcular(req))
                .isInstanceOf(CheckoutException.class)
                .extracting(e -> ((CheckoutException) e).getCodigo())
                .isEqualTo(CodigoErro.CUPOM_INVALIDO);
    }

    @Test
    void cupomNaoAplicavelQuandoAbaixoDoMinimo() {
        ItemRequest item = new ItemRequest("Meia", new BigDecimal("19.90"), 1, new BigDecimal("0.10"));
        ResumoRequest req = new ResumoRequest(
                List.of(item), "ECONOMICA", "MENOS50", "PIX", null, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcular(req))
                .isInstanceOf(CheckoutException.class)
                .extracting(e -> ((CheckoutException) e).getCodigo())
                .isEqualTo(CodigoErro.CUPOM_NAO_APLICAVEL);
    }

    @Test
    void formaPagamentoInvalidaEhDetectada() {
        ResumoRequest req = new ResumoRequest(
                List.of(camiseta()), "ECONOMICA", null, "CRIPTO", null, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcular(req))
                .isInstanceOf(CheckoutException.class)
                .extracting(e -> ((CheckoutException) e).getCodigo())
                .isEqualTo(CodigoErro.FORMA_PAGAMENTO_INVALIDA);
    }

    @Test
    void parcelamentoInvalidoParaPixComMaisDeUmaParcela() {
        ResumoRequest req = new ResumoRequest(
                List.of(camiseta()), "ECONOMICA", null, "PIX", 2, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcular(req))
                .isInstanceOf(CheckoutException.class)
                .extracting(e -> ((CheckoutException) e).getCodigo())
                .isEqualTo(CodigoErro.PARCELAMENTO_INVALIDO);
    }

    @Test
    void parcelamentoInvalidoParaCartaoAcimaDe12() {
        ResumoRequest req = new ResumoRequest(
                List.of(camiseta()), "ECONOMICA", null, "CARTAO", 13, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcular(req))
                .isInstanceOf(CheckoutException.class)
                .extracting(e -> ((CheckoutException) e).getCodigo())
                .isEqualTo(CodigoErro.PARCELAMENTO_INVALIDO);
    }

    @Test
    void boletoAcimaDeMilEhIndisponivel() {
        ItemRequest item = new ItemRequest("Notebook", new BigDecimal("1200.00"), 1, new BigDecimal("2.00"));
        ResumoRequest req = new ResumoRequest(
                List.of(item), "ECONOMICA", null, "BOLETO", null, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcular(req))
                .isInstanceOf(CheckoutException.class)
                .extracting(e -> ((CheckoutException) e).getCodigo())
                .isEqualTo(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
    }
}
