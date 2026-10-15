package com.loja.checkout.service;

import com.loja.checkout.api.CheckoutException;
import com.loja.checkout.api.dto.ItemRequest;
import com.loja.checkout.api.dto.ResumoRequest;
import com.loja.checkout.api.dto.ResumoResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CheckoutServiceTest {

    private final CheckoutService service = new CheckoutService();

    private static ItemRequest item(String nome, String preco, int quantidade, String peso) {
        return new ItemRequest(nome, new BigDecimal(preco), quantidade, new BigDecimal(peso));
    }

    @Test
    void exemplo1_expressaComBemvindo10EPix() {
        ResumoRequest request = new ResumoRequest(
                List.of(item("Camiseta", "79.90", 2, "0.30"), item("Tênis", "249.90", 1, "1.20")),
                "EXPRESSA", "BEMVINDO10", "PIX", 1, "BRONZE", "NORDESTE");

        ResumoResponse resumo = service.calcularResumo(request);

        assertThat(resumo.subtotalProdutos()).isEqualByComparingTo("409.70");
        assertThat(resumo.descontoCupom()).isEqualByComparingTo("40.97");
        assertThat(resumo.frete()).isEqualByComparingTo("33.10");
        assertThat(resumo.prazoEntregaDias()).isEqualTo(2);
    }

    @Test
    void exemplo2_economicaSemCupomCartao6x() {
        ResumoRequest request = new ResumoRequest(
                List.of(item("Camiseta", "79.90", 2, "0.30"), item("Tênis", "249.90", 1, "1.20")),
                "ECONOMICA", null, "CARTAO", 6, "BRONZE", "NORDESTE");

        ResumoResponse resumo = service.calcularResumo(request);

        assertThat(resumo.subtotalProdutos()).isEqualByComparingTo("409.70");
        assertThat(resumo.descontoCupom()).isEqualByComparingTo("0.00");
        assertThat(resumo.frete()).isEqualByComparingTo("15.60");
        assertThat(resumo.prazoEntregaDias()).isEqualTo(7);
    }

    @Test
    void exemplo3_motoboyComMenos50EBoleto() {
        ResumoRequest request = new ResumoRequest(
                List.of(item("Fone", "199.90", 2, "0.25")),
                "MOTOBOY", "MENOS50", "BOLETO", 1, "BRONZE", "NORDESTE");

        ResumoResponse resumo = service.calcularResumo(request);

        assertThat(resumo.subtotalProdutos()).isEqualByComparingTo("399.80");
        assertThat(resumo.descontoCupom()).isEqualByComparingTo("50.00");
        assertThat(resumo.frete()).isEqualByComparingTo("18.00");
        assertThat(resumo.prazoEntregaDias()).isEqualTo(0);
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo("3.49");
    }

    @Test
    void exemplo4_retiradaLojaComLeve3Pague2ECartao3x() {
        ResumoRequest request = new ResumoRequest(
                List.of(item("Meia", "19.90", 7, "0.10"), item("Camiseta", "79.90", 2, "0.30")),
                "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "BRONZE", "NORDESTE");

        ResumoResponse resumo = service.calcularResumo(request);

        assertThat(resumo.subtotalProdutos()).isEqualByComparingTo("299.10");
        assertThat(resumo.descontoCupom()).isEqualByComparingTo("39.80");
        assertThat(resumo.frete()).isEqualByComparingTo("0.00");
        assertThat(resumo.prazoEntregaDias()).isEqualTo(1);
        // CARTAO em ate 3x nao tem juros: ajuste zero e parcela = total do pedido / parcelas.
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo("0.00");
        assertThat(resumo.valorParcela()).isEqualByComparingTo(
                resumo.totalFinal().divide(new java.math.BigDecimal(3), 2, java.math.RoundingMode.HALF_EVEN));
    }

    @Test
    void exemplo5_expressaOuroSudestePix() {
        ResumoRequest request = new ResumoRequest(
                List.of(item("Camiseta", "79.90", 2, "0.30"), item("Tênis", "249.90", 1, "1.20")),
                "EXPRESSA", null, "PIX", 1, "OURO", "SUDESTE");

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

    @Test
    void enunciado_exemploDaChamada() {
        ResumoRequest request = new ResumoRequest(
                List.of(item("Camiseta", "79.90", 2, "0.30"), item("Tênis", "249.90", 1, "1.20")),
                "EXPRESSA", "BEMVINDO10", "PIX", 1, "OURO", "SUDESTE");

        ResumoResponse resumo = service.calcularResumo(request);

        assertThat(resumo.subtotalProdutos()).isEqualByComparingTo("409.70");
        assertThat(resumo.descontoCupom()).isEqualByComparingTo("40.97");
        assertThat(resumo.frete()).isEqualByComparingTo("0.00");
        assertThat(resumo.prazoEntregaDias()).isEqualTo(2);
    }

    @Test
    void carrinhoVazioRetornaPedidoInvalido() {
        ResumoRequest request = new ResumoRequest(
                List.of(), "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcularResumo(request))
                .isInstanceOf(CheckoutException.class)
                .extracting(e -> ((CheckoutException) e).codigo())
                .isEqualTo("PEDIDO_INVALIDO");
    }

    @Test
    void itemComQuantidadeZeroRetornaPedidoInvalido() {
        ResumoRequest request = new ResumoRequest(
                List.of(item("Camiseta", "79.90", 0, "0.30")), "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcularResumo(request))
                .isInstanceOf(CheckoutException.class)
                .extracting(e -> ((CheckoutException) e).codigo())
                .isEqualTo("PEDIDO_INVALIDO");
    }

    @Test
    void nivelClubeInvalidoRetornaErroAntesDosDemais() {
        ResumoRequest request = new ResumoRequest(
                List.of(item("Camiseta", "79.90", 1, "0.30")), "MODALIDADE_INEXISTENTE", "CUPOM_INEXISTENTE",
                "FORMA_INEXISTENTE", 1, "PLATINA", "PLANETA");

        assertThatThrownBy(() -> service.calcularResumo(request))
                .isInstanceOf(CheckoutException.class)
                .extracting(e -> ((CheckoutException) e).codigo())
                .isEqualTo("NIVEL_CLUBE_INVALIDO");
    }

    @Test
    void regiaoInvalidaRetornaErroAntesDeModalidade() {
        ResumoRequest request = new ResumoRequest(
                List.of(item("Camiseta", "79.90", 1, "0.30")), "MODALIDADE_INEXISTENTE", null,
                "PIX", 1, "BRONZE", "PLANETA");

        assertThatThrownBy(() -> service.calcularResumo(request))
                .isInstanceOf(CheckoutException.class)
                .extracting(e -> ((CheckoutException) e).codigo())
                .isEqualTo("REGIAO_INVALIDA");
    }

    @Test
    void modalidadeInvalidaRetornaErro() {
        ResumoRequest request = new ResumoRequest(
                List.of(item("Camiseta", "79.90", 1, "0.30")), "TELEPORTE", null,
                "PIX", 1, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcularResumo(request))
                .isInstanceOf(CheckoutException.class)
                .extracting(e -> ((CheckoutException) e).codigo())
                .isEqualTo("MODALIDADE_INVALIDA");
    }

    @Test
    void motoboyAcimaDoLimiteRetornaModalidadeIndisponivel() {
        ResumoRequest request = new ResumoRequest(
                List.of(item("Sofá", "999.90", 1, "6.00")), "MOTOBOY", null,
                "PIX", 1, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcularResumo(request))
                .isInstanceOf(CheckoutException.class)
                .extracting(e -> ((CheckoutException) e).codigo())
                .isEqualTo("MODALIDADE_INDISPONIVEL");
    }

    @Test
    void cupomInvalidoRetornaErro() {
        ResumoRequest request = new ResumoRequest(
                List.of(item("Camiseta", "79.90", 1, "0.30")), "EXPRESSA", "NAOEXISTE",
                "PIX", 1, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcularResumo(request))
                .isInstanceOf(CheckoutException.class)
                .extracting(e -> ((CheckoutException) e).codigo())
                .isEqualTo("CUPOM_INVALIDO");
    }

    @Test
    void cupomNaoAplicavelRetornaErro() {
        ResumoRequest request = new ResumoRequest(
                List.of(item("Camiseta", "79.90", 1, "0.30")), "EXPRESSA", "MENOS50",
                "PIX", 1, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcularResumo(request))
                .isInstanceOf(CheckoutException.class)
                .extracting(e -> ((CheckoutException) e).codigo())
                .isEqualTo("CUPOM_NAO_APLICAVEL");
    }

    @Test
    void formaPagamentoInvalidaRetornaErro() {
        ResumoRequest request = new ResumoRequest(
                List.of(item("Camiseta", "79.90", 1, "0.30")), "EXPRESSA", null,
                "DINHEIRO", 1, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcularResumo(request))
                .isInstanceOf(CheckoutException.class)
                .extracting(e -> ((CheckoutException) e).codigo())
                .isEqualTo("FORMA_PAGAMENTO_INVALIDA");
    }

    @Test
    void parcelamentoInvalidoParaPixRetornaErro() {
        ResumoRequest request = new ResumoRequest(
                List.of(item("Camiseta", "79.90", 1, "0.30")), "EXPRESSA", null,
                "PIX", 2, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcularResumo(request))
                .isInstanceOf(CheckoutException.class)
                .extracting(e -> ((CheckoutException) e).codigo())
                .isEqualTo("PARCELAMENTO_INVALIDO");
    }

    @Test
    void parcelamentoInvalidoParaCartaoAcimaDe12RetornaErro() {
        ResumoRequest request = new ResumoRequest(
                List.of(item("Camiseta", "79.90", 1, "0.30")), "EXPRESSA", null,
                "CARTAO", 13, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcularResumo(request))
                .isInstanceOf(CheckoutException.class)
                .extracting(e -> ((CheckoutException) e).codigo())
                .isEqualTo("PARCELAMENTO_INVALIDO");
    }

    @Test
    void boletoAcimaDoLimiteRetornaFormaPagamentoIndisponivel() {
        ResumoRequest request = new ResumoRequest(
                List.of(item("Notebook", "1200.00", 1, "2.00")), "RETIRADA_LOJA", null,
                "BOLETO", 1, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcularResumo(request))
                .isInstanceOf(CheckoutException.class)
                .extracting(e -> ((CheckoutException) e).codigo())
                .isEqualTo("FORMA_PAGAMENTO_INDISPONIVEL");
    }
}
