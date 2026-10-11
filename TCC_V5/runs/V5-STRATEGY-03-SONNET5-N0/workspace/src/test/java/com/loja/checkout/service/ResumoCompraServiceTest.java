package com.loja.checkout.service;

import com.loja.checkout.api.ErroPedidoException;
import com.loja.checkout.api.dto.ItemRequest;
import com.loja.checkout.api.dto.ResumoRequest;
import com.loja.checkout.api.dto.ResumoResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResumoCompraServiceTest {

    private final ResumoCompraService service = new ResumoCompraService();

    private static ItemRequest camiseta(int quantidade) {
        return new ItemRequest("Camiseta", new BigDecimal("79.90"), quantidade, new BigDecimal("0.30"));
    }

    private static ItemRequest tenis(int quantidade) {
        return new ItemRequest("Tenis", new BigDecimal("249.90"), quantidade, new BigDecimal("1.20"));
    }

    private static ItemRequest fone(int quantidade) {
        return new ItemRequest("Fone", new BigDecimal("199.90"), quantidade, new BigDecimal("0.25"));
    }

    private static ItemRequest meia(int quantidade) {
        return new ItemRequest("Meia", new BigDecimal("19.90"), quantidade, new BigDecimal("0.10"));
    }

    @Test
    void exemplo1_expressaBemvindo10PixBronzeNorte() {
        ResumoRequest request = new ResumoRequest(
                List.of(camiseta(2), tenis(1)),
                "EXPRESSA", "BEMVINDO10", "PIX", 1, "BRONZE", "NORTE");

        ResumoResponse resumo = service.calcular(request);

        assertThat(resumo.subtotalProdutos()).isEqualTo("409.70");
        assertThat(resumo.descontoCupom()).isEqualTo("40.97");
        assertThat(resumo.frete()).isEqualTo("33.10");
        assertThat(resumo.prazoEntregaDias()).isEqualTo(2);
        assertThat(resumo.seguro()).isEqualTo("10.24");
        assertThat(resumo.ajustePagamento()).isEqualTo("-20.60");
        assertThat(resumo.totalFinal()).isEqualTo("391.47");
        assertThat(resumo.parcelas()).isEqualTo(1);
        assertThat(resumo.valorParcela()).isEqualTo("391.47");
        assertThat(resumo.creditoProximaCompra()).isEqualTo("0.00");
        assertThat(resumo.brinde()).isFalse();
    }

    @Test
    void exemplo2_economicaSemCupomCartao6xPrataCentroOeste() {
        ResumoRequest request = new ResumoRequest(
                List.of(camiseta(2), tenis(1)),
                "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE");

        ResumoResponse resumo = service.calcular(request);

        assertThat(resumo.subtotalProdutos()).isEqualTo("409.70");
        assertThat(resumo.descontoCupom()).isEqualTo("0.00");
        assertThat(resumo.frete()).isEqualTo("15.60");
        assertThat(resumo.prazoEntregaDias()).isEqualTo(7);
        assertThat(resumo.seguro()).isEqualTo("6.15");
        assertThat(resumo.ajustePagamento()).isEqualTo("30.55");
        assertThat(resumo.totalFinal()).isEqualTo("462.00");
        assertThat(resumo.parcelas()).isEqualTo(6);
        assertThat(resumo.valorParcela()).isEqualTo("77.00");
        assertThat(resumo.creditoProximaCompra()).isEqualTo("8.19");
        assertThat(resumo.brinde()).isFalse();
    }

    @Test
    void exemplo3_motoboyMenos50BoletoBronzeNordeste() {
        ResumoRequest request = new ResumoRequest(
                List.of(fone(2)),
                "MOTOBOY", "MENOS50", "BOLETO", null, "BRONZE", "NORDESTE");

        ResumoResponse resumo = service.calcular(request);

        assertThat(resumo.subtotalProdutos()).isEqualTo("399.80");
        assertThat(resumo.descontoCupom()).isEqualTo("50.00");
        assertThat(resumo.frete()).isEqualTo("18.00");
        assertThat(resumo.prazoEntregaDias()).isEqualTo(0);
        assertThat(resumo.seguro()).isEqualTo("8.00");
        assertThat(resumo.ajustePagamento()).isEqualTo("3.49");
        assertThat(resumo.totalFinal()).isEqualTo("379.29");
        assertThat(resumo.parcelas()).isEqualTo(1);
        assertThat(resumo.valorParcela()).isEqualTo("379.29");
        assertThat(resumo.creditoProximaCompra()).isEqualTo("0.00");
        assertThat(resumo.brinde()).isFalse();
    }

    @Test
    void exemplo4_retiradaLojaLeve3Pague2Cartao3xPrataSul() {
        ResumoRequest request = new ResumoRequest(
                List.of(meia(7), camiseta(2)),
                "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL");

        ResumoResponse resumo = service.calcular(request);

        assertThat(resumo.subtotalProdutos()).isEqualTo("299.10");
        assertThat(resumo.descontoCupom()).isEqualTo("39.80");
        assertThat(resumo.frete()).isEqualTo("0.00");
        assertThat(resumo.prazoEntregaDias()).isEqualTo(1);
        assertThat(resumo.seguro()).isEqualTo("2.99");
        assertThat(resumo.ajustePagamento()).isEqualTo("0.00");
        assertThat(resumo.totalFinal()).isEqualTo("262.29");
        assertThat(resumo.parcelas()).isEqualTo(3);
        assertThat(resumo.valorParcela()).isEqualTo("87.43");
        assertThat(resumo.creditoProximaCompra()).isEqualTo("5.98");
        assertThat(resumo.brinde()).isFalse();
    }

    @Test
    void exemplo5_expressaSemCupomPixOuroSudeste() {
        ResumoRequest request = new ResumoRequest(
                List.of(camiseta(2), tenis(1)),
                "EXPRESSA", null, "PIX", null, "OURO", "SUDESTE");

        ResumoResponse resumo = service.calcular(request);

        assertThat(resumo.subtotalProdutos()).isEqualTo("409.70");
        assertThat(resumo.descontoCupom()).isEqualTo("0.00");
        assertThat(resumo.frete()).isEqualTo("0.00");
        assertThat(resumo.prazoEntregaDias()).isEqualTo(2);
        assertThat(resumo.seguro()).isEqualTo("4.10");
        assertThat(resumo.ajustePagamento()).isEqualTo("-20.69");
        assertThat(resumo.totalFinal()).isEqualTo("393.11");
        assertThat(resumo.parcelas()).isEqualTo(1);
        assertThat(resumo.valorParcela()).isEqualTo("393.11");
        assertThat(resumo.creditoProximaCompra()).isEqualTo("20.48");
        assertThat(resumo.brinde()).isFalse();
    }

    @Test
    void carrinhoVazioDevolvePedidoInvalido() {
        ResumoRequest request = new ResumoRequest(
                List.of(), "EXPRESSA", null, "PIX", null, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcular(request))
                .isInstanceOf(ErroPedidoException.class)
                .extracting(e -> ((ErroPedidoException) e).getCodigo())
                .isEqualTo("PEDIDO_INVALIDO");
    }

    @Test
    void motoboyAcimaDoLimiteDevolveModalidadeIndisponivel() {
        ResumoRequest request = new ResumoRequest(
                List.of(new ItemRequest("Caixa", new BigDecimal("50.00"), 1, new BigDecimal("6.00"))),
                "MOTOBOY", null, "PIX", null, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcular(request))
                .isInstanceOf(ErroPedidoException.class)
                .extracting(e -> ((ErroPedidoException) e).getCodigo())
                .isEqualTo("MODALIDADE_INDISPONIVEL");
    }

    @Test
    void menos50AbaixoDoMinimoDevolveCupomNaoAplicavel() {
        ResumoRequest request = new ResumoRequest(
                List.of(meia(1)),
                "RETIRADA_LOJA", "MENOS50", "PIX", null, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcular(request))
                .isInstanceOf(ErroPedidoException.class)
                .extracting(e -> ((ErroPedidoException) e).getCodigo())
                .isEqualTo("CUPOM_NAO_APLICAVEL");
    }

    @Test
    void boletoAcimaDoLimiteDevolveFormaPagamentoIndisponivel() {
        ResumoRequest request = new ResumoRequest(
                List.of(tenis(5)),
                "RETIRADA_LOJA", null, "BOLETO", null, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcular(request))
                .isInstanceOf(ErroPedidoException.class)
                .extracting(e -> ((ErroPedidoException) e).getCodigo())
                .isEqualTo("FORMA_PAGAMENTO_INDISPONIVEL");
    }

    @Test
    void pixComParcelasMaiorQueUmDevolveParcelamentoInvalido() {
        ResumoRequest request = new ResumoRequest(
                List.of(meia(1)),
                "RETIRADA_LOJA", null, "PIX", 2, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcular(request))
                .isInstanceOf(ErroPedidoException.class)
                .extracting(e -> ((ErroPedidoException) e).getCodigo())
                .isEqualTo("PARCELAMENTO_INVALIDO");
    }
}
