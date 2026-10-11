package com.loja.checkout.domain;

import com.loja.checkout.api.dto.CheckoutRequest;
import com.loja.checkout.api.dto.CheckoutResponse;
import com.loja.checkout.api.dto.ItemRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class ResumoCheckoutServiceTest {

    @Autowired
    ResumoCheckoutService service;

    private static ItemRequest item(String nome, String preco, int qtd, String peso) {
        return new ItemRequest(nome, new BigDecimal(preco), qtd, new BigDecimal(peso));
    }

    private static final ItemRequest CAMISETA = item("Camiseta", "79.90", 2, "0.30");
    private static final ItemRequest TENIS = item("Tenis", "249.90", 1, "1.20");

    @Test
    void exemplo1() {
        CheckoutResponse r = service.resumir(new CheckoutRequest(
                List.of(CAMISETA, TENIS), "EXPRESSA", "BEMVINDO10", "PIX", null, "BRONZE", "NORTE"));

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
        CheckoutResponse r = service.resumir(new CheckoutRequest(
                List.of(CAMISETA, TENIS), "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE"));

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
        CheckoutResponse r = service.resumir(new CheckoutRequest(
                List.of(item("Fone", "199.90", 2, "0.25")), "MOTOBOY", "MENOS50", "BOLETO", null,
                "BRONZE", "NORDESTE"));

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
        CheckoutResponse r = service.resumir(new CheckoutRequest(
                List.of(item("Meia", "19.90", 7, "0.10"), CAMISETA), "RETIRADA_LOJA", "LEVE3PAGUE2",
                "CARTAO", 3, "PRATA", "SUL"));

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
        CheckoutResponse r = service.resumir(new CheckoutRequest(
                List.of(CAMISETA, TENIS), "EXPRESSA", null, "PIX", null, "OURO", "SUDESTE"));

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
    void ouroComProdutosAcimaDe500GanhaBrinde() {
        CheckoutResponse r = service.resumir(new CheckoutRequest(
                List.of(item("Jaqueta", "600.00", 1, "1.00")), "RETIRADA_LOJA", null, "PIX", null,
                "OURO", "SUDESTE"));
        assertThat(r.brinde()).isTrue();
    }

    private CheckoutRequest comErro(List<ItemRequest> itens, String modalidade, String cupom,
                                    String forma, Integer parcelas, String nivel, String regiao) {
        return new CheckoutRequest(itens, modalidade, cupom, forma, parcelas, nivel, regiao);
    }

    private void assertErro(CheckoutRequest req, ErroCheckout esperado) {
        assertThatThrownBy(() -> service.resumir(req))
                .isInstanceOf(CheckoutException.class)
                .extracting(e -> ((CheckoutException) e).getErro())
                .isEqualTo(esperado);
    }

    @Test
    void carrinhoVazio() {
        assertErro(comErro(List.of(), "EXPRESSA", null, "PIX", null, "BRONZE", "NORTE"),
                ErroCheckout.PEDIDO_INVALIDO);
    }

    @Test
    void itemComPrecoZero() {
        assertErro(comErro(List.of(item("X", "0.00", 1, "0.10")), "EXPRESSA", null, "PIX", null,
                "BRONZE", "NORTE"), ErroCheckout.PEDIDO_INVALIDO);
    }

    @Test
    void nivelInvalido() {
        assertErro(comErro(List.of(CAMISETA), "EXPRESSA", null, "PIX", null, "DIAMANTE", "NORTE"),
                ErroCheckout.NIVEL_CLUBE_INVALIDO);
    }

    @Test
    void regiaoInvalida() {
        assertErro(comErro(List.of(CAMISETA), "EXPRESSA", null, "PIX", null, "BRONZE", "LESTE"),
                ErroCheckout.REGIAO_INVALIDA);
    }

    @Test
    void modalidadeInvalida() {
        assertErro(comErro(List.of(CAMISETA), "DRONE", null, "PIX", null, "BRONZE", "NORTE"),
                ErroCheckout.MODALIDADE_INVALIDA);
    }

    @Test
    void motoboyAcimaDoLimite() {
        assertErro(comErro(List.of(item("Haltere", "100.00", 1, "6.00")), "MOTOBOY", null, "PIX",
                null, "BRONZE", "NORTE"), ErroCheckout.MODALIDADE_INDISPONIVEL);
    }

    @Test
    void cupomInvalido() {
        assertErro(comErro(List.of(CAMISETA), "EXPRESSA", "XPTO", "PIX", null, "BRONZE", "NORTE"),
                ErroCheckout.CUPOM_INVALIDO);
    }

    @Test
    void cupomNaoAplicavel() {
        assertErro(comErro(List.of(item("Meia", "19.90", 1, "0.10")), "EXPRESSA", "MENOS50", "PIX",
                null, "BRONZE", "NORTE"), ErroCheckout.CUPOM_NAO_APLICAVEL);
    }

    @Test
    void formaPagamentoInvalida() {
        assertErro(comErro(List.of(CAMISETA), "EXPRESSA", null, "CHEQUE", null, "BRONZE", "NORTE"),
                ErroCheckout.FORMA_PAGAMENTO_INVALIDA);
    }

    @Test
    void parcelamentoInvalidoNoPix() {
        assertErro(comErro(List.of(CAMISETA), "EXPRESSA", null, "PIX", 2, "BRONZE", "NORTE"),
                ErroCheckout.PARCELAMENTO_INVALIDO);
    }

    @Test
    void boletoAcimaDoTeto() {
        assertErro(comErro(List.of(item("Sofa", "600.00", 2, "0.50")), "RETIRADA_LOJA", null,
                "BOLETO", null, "BRONZE", "SUDESTE"), ErroCheckout.FORMA_PAGAMENTO_INDISPONIVEL);
    }

    @Test
    void confereNaOrdemDefinida() {
        // Nível e região inválidos ao mesmo tempo: vence o nível, que é conferido antes.
        assertErro(comErro(List.of(CAMISETA), "EXPRESSA", null, "PIX", null, "DIAMANTE", "LESTE"),
                ErroCheckout.NIVEL_CLUBE_INVALIDO);
    }
}
