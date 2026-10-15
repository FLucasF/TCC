package com.loja.checkout.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.loja.checkout.domain.CheckoutException;
import com.loja.checkout.web.CheckoutRequest;
import com.loja.checkout.web.CheckoutResponse;
import com.loja.checkout.web.ItemRequest;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class CheckoutServiceTest {

    private final CheckoutService service = new CheckoutService();

    private static ItemRequest item(String nome, String preco, int qtd, String peso) {
        return new ItemRequest(nome, new BigDecimal(preco), qtd, new BigDecimal(peso));
    }

    private static BigDecimal brl(String v) {
        return new BigDecimal(v);
    }

    private static List<ItemRequest> camisetaETenis() {
        return List.of(item("Camiseta", "79.90", 2, "0.30"), item("Tenis", "249.90", 1, "1.20"));
    }

    @Test
    void exemplo1() {
        CheckoutRequest req = new CheckoutRequest(
                camisetaETenis(), "EXPRESSA", "BEMVINDO10", "PIX", 1, "BRONZE", "NORTE");

        CheckoutResponse r = service.calcular(req);

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
    void exemplo2() {
        CheckoutRequest req = new CheckoutRequest(
                camisetaETenis(), "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE");

        CheckoutResponse r = service.calcular(req);

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
    void exemplo3() {
        CheckoutRequest req = new CheckoutRequest(
                List.of(item("Fone", "199.90", 2, "0.25")),
                "MOTOBOY", "MENOS50", "BOLETO", null, "BRONZE", "NORDESTE");

        CheckoutResponse r = service.calcular(req);

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
    void exemplo4() {
        CheckoutRequest req = new CheckoutRequest(
                List.of(item("Meia", "19.90", 7, "0.10"), item("Camiseta", "79.90", 2, "0.30")),
                "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL");

        CheckoutResponse r = service.calcular(req);

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
    void exemplo5() {
        CheckoutRequest req = new CheckoutRequest(
                camisetaETenis(), "EXPRESSA", null, "PIX", null, "OURO", "SUDESTE");

        CheckoutResponse r = service.calcular(req);

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
    void brindeOuroAcimaDe500() {
        CheckoutRequest req = new CheckoutRequest(
                List.of(item("Jaqueta", "600.00", 1, "0.80")),
                "RETIRADA_LOJA", null, "PIX", 1, "OURO", "SUDESTE");

        CheckoutResponse r = service.calcular(req);

        assertThat(r.brinde()).isTrue();
        assertThat(r.frete()).isEqualByComparingTo("0.00");
        assertThat(r.creditoProximaCompra()).isEqualByComparingTo("30.00");
    }

    @Test
    void valoresMonetariosComDuasCasas() {
        CheckoutResponse r = service.calcular(new CheckoutRequest(
                camisetaETenis(), "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE"));

        assertThat(r.subtotalProdutos().scale()).isEqualTo(2);
        assertThat(r.frete().scale()).isEqualTo(2);
        assertThat(r.seguro().scale()).isEqualTo(2);
        assertThat(r.totalFinal().scale()).isEqualTo(2);
        assertThat(r.valorParcela().scale()).isEqualTo(2);
        assertThat(r.ajustePagamento().scale()).isEqualTo(2);
        assertThat(r.descontoCupom().scale()).isEqualTo(2);
        assertThat(r.creditoProximaCompra().scale()).isEqualTo(2);
    }

    // ---- Erros, na ordem de verificação ----

    private CheckoutRequest valido() {
        return new CheckoutRequest(camisetaETenis(), "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE");
    }

    private void esperaErro(CheckoutRequest req, String codigo) {
        assertThatThrownBy(() -> service.calcular(req))
                .isInstanceOf(CheckoutException.class)
                .extracting(e -> ((CheckoutException) e).getCodigo())
                .isEqualTo(codigo);
    }

    @Test
    void carrinhoVazio() {
        esperaErro(new CheckoutRequest(List.of(), "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE"),
                "PEDIDO_INVALIDO");
    }

    @Test
    void itemComQuantidadeZero() {
        esperaErro(new CheckoutRequest(List.of(item("X", "10.00", 0, "0.10")),
                "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE"), "PEDIDO_INVALIDO");
    }

    @Test
    void itemComPrecoNegativo() {
        esperaErro(new CheckoutRequest(List.of(item("X", "-1.00", 1, "0.10")),
                "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE"), "PEDIDO_INVALIDO");
    }

    @Test
    void itemSemPeso() {
        esperaErro(new CheckoutRequest(List.of(new ItemRequest("X", brl("10.00"), 1, null)),
                "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE"), "PEDIDO_INVALIDO");
    }

    @Test
    void nivelClubeInvalido() {
        esperaErro(new CheckoutRequest(camisetaETenis(), "EXPRESSA", null, "PIX", 1, "DIAMANTE", "SUDESTE"),
                "NIVEL_CLUBE_INVALIDO");
    }

    @Test
    void regiaoInvalida() {
        esperaErro(new CheckoutRequest(camisetaETenis(), "EXPRESSA", null, "PIX", 1, "BRONZE", "EXTERIOR"),
                "REGIAO_INVALIDA");
    }

    @Test
    void modalidadeInvalida() {
        esperaErro(new CheckoutRequest(camisetaETenis(), "DRONE", null, "PIX", 1, "BRONZE", "SUDESTE"),
                "MODALIDADE_INVALIDA");
    }

    @Test
    void motoboyAcimaDe5kg() {
        esperaErro(new CheckoutRequest(List.of(item("Peso", "10.00", 1, "6.00")),
                "MOTOBOY", null, "PIX", 1, "BRONZE", "SUDESTE"), "MODALIDADE_INDISPONIVEL");
    }

    @Test
    void motoboyExatamente5kgEhPermitido() {
        CheckoutResponse r = service.calcular(new CheckoutRequest(
                List.of(item("Peso", "10.00", 5, "1.00")),
                "MOTOBOY", null, "PIX", 1, "BRONZE", "SUDESTE"));
        assertThat(r.frete()).isEqualByComparingTo("18.00");
    }

    @Test
    void cupomInexistente() {
        esperaErro(new CheckoutRequest(camisetaETenis(), "EXPRESSA", "NATAL99", "PIX", 1, "BRONZE", "SUDESTE"),
                "CUPOM_INVALIDO");
    }

    @Test
    void menos50AbaixoDe300() {
        esperaErro(new CheckoutRequest(List.of(item("X", "100.00", 1, "0.10")),
                "EXPRESSA", "MENOS50", "PIX", 1, "BRONZE", "SUDESTE"), "CUPOM_NAO_APLICAVEL");
    }

    @Test
    void formaPagamentoInvalida() {
        esperaErro(new CheckoutRequest(camisetaETenis(), "EXPRESSA", null, "CRIPTO", 1, "BRONZE", "SUDESTE"),
                "FORMA_PAGAMENTO_INVALIDA");
    }

    @Test
    void pixParceladoNaoPermitido() {
        esperaErro(new CheckoutRequest(camisetaETenis(), "EXPRESSA", null, "PIX", 2, "BRONZE", "SUDESTE"),
                "PARCELAMENTO_INVALIDO");
    }

    @Test
    void cartaoAcimaDe12x() {
        esperaErro(new CheckoutRequest(camisetaETenis(), "EXPRESSA", null, "CARTAO", 13, "BRONZE", "SUDESTE"),
                "PARCELAMENTO_INVALIDO");
    }

    @Test
    void boletoAcimaDe1000() {
        esperaErro(new CheckoutRequest(List.of(item("Caro", "1200.00", 1, "0.50")),
                "RETIRADA_LOJA", null, "BOLETO", 1, "BRONZE", "SUDESTE"), "FORMA_PAGAMENTO_INDISPONIVEL");
    }

    @Test
    void ordemErroPedidoAntesDeNivel() {
        // Carrinho vazio E nível inválido: vale o primeiro da ordem (PEDIDO_INVALIDO).
        esperaErro(new CheckoutRequest(List.of(), "EXPRESSA", null, "PIX", 1, "DIAMANTE", "SUDESTE"),
                "PEDIDO_INVALIDO");
    }

    @Test
    void ordemErroCupomAntesDePagamento() {
        // Cupom inválido E forma de pagamento inválida: vale o cupom.
        esperaErro(new CheckoutRequest(camisetaETenis(), "EXPRESSA", "XPTO", "CRIPTO", 1, "BRONZE", "SUDESTE"),
                "CUPOM_INVALIDO");
    }
}
