package com.loja.checkout.service;

import com.loja.checkout.web.CheckoutRequest;
import com.loja.checkout.web.ItemPedido;
import com.loja.checkout.web.ResumoCompra;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CheckoutServiceTest {

    private final CheckoutService service = new CheckoutService();

    private static ItemPedido item(String nome, String preco, int qtd, String peso) {
        return new ItemPedido(nome, new BigDecimal(preco), qtd, new BigDecimal(peso));
    }

    private static BigDecimal r(String v) {
        return new BigDecimal(v);
    }

    private static final List<ItemPedido> CAMISETA_E_TENIS = List.of(
            item("Camiseta", "79.90", 2, "0.30"),
            item("Tenis", "249.90", 1, "1.20"));

    @Test
    void exemplo1() {
        ResumoCompra resumo = service.calcular(new CheckoutRequest(
                CAMISETA_E_TENIS, "EXPRESSA", "BEMVINDO10", "PIX", 1, "BRONZE", "NORTE"));

        assertThat(resumo.subtotalProdutos()).isEqualByComparingTo(r("409.70"));
        assertThat(resumo.descontoCupom()).isEqualByComparingTo(r("40.97"));
        assertThat(resumo.frete()).isEqualByComparingTo(r("33.10"));
        assertThat(resumo.prazoEntregaDias()).isEqualTo(2);
        assertThat(resumo.seguro()).isEqualByComparingTo(r("10.24"));
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo(r("-20.60"));
        assertThat(resumo.totalFinal()).isEqualByComparingTo(r("391.47"));
        assertThat(resumo.parcelas()).isEqualTo(1);
        assertThat(resumo.valorParcela()).isEqualByComparingTo(r("391.47"));
        assertThat(resumo.creditoProximaCompra()).isEqualByComparingTo(r("0.00"));
        assertThat(resumo.brinde()).isFalse();
    }

    @Test
    void exemplo2() {
        ResumoCompra resumo = service.calcular(new CheckoutRequest(
                CAMISETA_E_TENIS, "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE"));

        assertThat(resumo.subtotalProdutos()).isEqualByComparingTo(r("409.70"));
        assertThat(resumo.descontoCupom()).isEqualByComparingTo(r("0.00"));
        assertThat(resumo.frete()).isEqualByComparingTo(r("15.60"));
        assertThat(resumo.prazoEntregaDias()).isEqualTo(7);
        assertThat(resumo.seguro()).isEqualByComparingTo(r("6.15"));
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo(r("30.55"));
        assertThat(resumo.totalFinal()).isEqualByComparingTo(r("462.00"));
        assertThat(resumo.parcelas()).isEqualTo(6);
        assertThat(resumo.valorParcela()).isEqualByComparingTo(r("77.00"));
        assertThat(resumo.creditoProximaCompra()).isEqualByComparingTo(r("8.19"));
        assertThat(resumo.brinde()).isFalse();
    }

    @Test
    void exemplo3() {
        ResumoCompra resumo = service.calcular(new CheckoutRequest(
                List.of(item("Fone", "199.90", 2, "0.25")),
                "MOTOBOY", "MENOS50", "BOLETO", null, "BRONZE", "NORDESTE"));

        assertThat(resumo.subtotalProdutos()).isEqualByComparingTo(r("399.80"));
        assertThat(resumo.descontoCupom()).isEqualByComparingTo(r("50.00"));
        assertThat(resumo.frete()).isEqualByComparingTo(r("18.00"));
        assertThat(resumo.prazoEntregaDias()).isEqualTo(0);
        assertThat(resumo.seguro()).isEqualByComparingTo(r("8.00"));
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo(r("3.49"));
        assertThat(resumo.totalFinal()).isEqualByComparingTo(r("379.29"));
        assertThat(resumo.parcelas()).isEqualTo(1);
        assertThat(resumo.valorParcela()).isEqualByComparingTo(r("379.29"));
        assertThat(resumo.creditoProximaCompra()).isEqualByComparingTo(r("0.00"));
        assertThat(resumo.brinde()).isFalse();
    }

    @Test
    void exemplo4() {
        ResumoCompra resumo = service.calcular(new CheckoutRequest(
                List.of(item("Meia", "19.90", 7, "0.10"), item("Camiseta", "79.90", 2, "0.30")),
                "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL"));

        assertThat(resumo.subtotalProdutos()).isEqualByComparingTo(r("299.10"));
        assertThat(resumo.descontoCupom()).isEqualByComparingTo(r("39.80"));
        assertThat(resumo.frete()).isEqualByComparingTo(r("0.00"));
        assertThat(resumo.prazoEntregaDias()).isEqualTo(1);
        assertThat(resumo.seguro()).isEqualByComparingTo(r("2.99"));
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo(r("0.00"));
        assertThat(resumo.totalFinal()).isEqualByComparingTo(r("262.29"));
        assertThat(resumo.parcelas()).isEqualTo(3);
        assertThat(resumo.valorParcela()).isEqualByComparingTo(r("87.43"));
        assertThat(resumo.creditoProximaCompra()).isEqualByComparingTo(r("5.98"));
        assertThat(resumo.brinde()).isFalse();
    }

    @Test
    void exemplo5() {
        ResumoCompra resumo = service.calcular(new CheckoutRequest(
                CAMISETA_E_TENIS, "EXPRESSA", null, "PIX", 1, "OURO", "SUDESTE"));

        assertThat(resumo.subtotalProdutos()).isEqualByComparingTo(r("409.70"));
        assertThat(resumo.descontoCupom()).isEqualByComparingTo(r("0.00"));
        assertThat(resumo.frete()).isEqualByComparingTo(r("0.00"));
        assertThat(resumo.prazoEntregaDias()).isEqualTo(2);
        assertThat(resumo.seguro()).isEqualByComparingTo(r("4.10"));
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo(r("-20.69"));
        assertThat(resumo.totalFinal()).isEqualByComparingTo(r("393.11"));
        assertThat(resumo.parcelas()).isEqualTo(1);
        assertThat(resumo.valorParcela()).isEqualByComparingTo(r("393.11"));
        assertThat(resumo.creditoProximaCompra()).isEqualByComparingTo(r("20.48"));
        assertThat(resumo.brinde()).isFalse();
    }

    @Test
    void brindeQuandoOuroPassaDe500() {
        ResumoCompra resumo = service.calcular(new CheckoutRequest(
                List.of(item("Casaco", "300.00", 2, "0.80")),
                "RETIRADA_LOJA", null, "PIX", 1, "OURO", "SUDESTE"));

        assertThat(resumo.brinde()).isTrue();
    }

    private String erroDe(CheckoutRequest req) {
        return org.assertj.core.api.Assertions.catchThrowableOfType(
                CheckoutException.class, () -> service.calcular(req)).getCodigo();
    }

    @Test
    void pedidoInvalido() {
        assertThat(erroDe(new CheckoutRequest(
                List.of(), "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE"))).isEqualTo("PEDIDO_INVALIDO");
        assertThat(erroDe(new CheckoutRequest(
                List.of(item("X", "0", 1, "0.10")), "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE")))
                .isEqualTo("PEDIDO_INVALIDO");
        assertThat(erroDe(new CheckoutRequest(
                List.of(new ItemPedido("X", new BigDecimal("10"), null, new BigDecimal("0.10"))),
                "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE"))).isEqualTo("PEDIDO_INVALIDO");
    }

    @Test
    void demaisErros() {
        List<ItemPedido> ok = List.of(item("X", "10.00", 1, "0.10"));
        List<ItemPedido> pesado = List.of(item("X", "10.00", 1, "6.00"));
        List<ItemPedido> caro = List.of(item("X", "2000.00", 1, "0.10"));

        assertThat(erroDe(new CheckoutRequest(ok, "EXPRESSA", null, "PIX", 1, "DIAMANTE", "SUDESTE")))
                .isEqualTo("NIVEL_CLUBE_INVALIDO");
        assertThat(erroDe(new CheckoutRequest(ok, "EXPRESSA", null, "PIX", 1, "BRONZE", "LESTE")))
                .isEqualTo("REGIAO_INVALIDA");
        assertThat(erroDe(new CheckoutRequest(ok, "DRONE", null, "PIX", 1, "BRONZE", "SUDESTE")))
                .isEqualTo("MODALIDADE_INVALIDA");
        assertThat(erroDe(new CheckoutRequest(pesado, "MOTOBOY", null, "PIX", 1, "BRONZE", "SUDESTE")))
                .isEqualTo("MODALIDADE_INDISPONIVEL");
        assertThat(erroDe(new CheckoutRequest(ok, "EXPRESSA", "INEXISTENTE", "PIX", 1, "BRONZE", "SUDESTE")))
                .isEqualTo("CUPOM_INVALIDO");
        assertThat(erroDe(new CheckoutRequest(ok, "EXPRESSA", "MENOS50", "PIX", 1, "BRONZE", "SUDESTE")))
                .isEqualTo("CUPOM_NAO_APLICAVEL");
        assertThat(erroDe(new CheckoutRequest(ok, "EXPRESSA", null, "CHEQUE", 1, "BRONZE", "SUDESTE")))
                .isEqualTo("FORMA_PAGAMENTO_INVALIDA");
        assertThat(erroDe(new CheckoutRequest(ok, "EXPRESSA", null, "PIX", 2, "BRONZE", "SUDESTE")))
                .isEqualTo("PARCELAMENTO_INVALIDO");
        assertThat(erroDe(new CheckoutRequest(ok, "EXPRESSA", null, "CARTAO", 13, "BRONZE", "SUDESTE")))
                .isEqualTo("PARCELAMENTO_INVALIDO");
        assertThat(erroDe(new CheckoutRequest(caro, "RETIRADA_LOJA", null, "BOLETO", 1, "BRONZE", "SUDESTE")))
                .isEqualTo("FORMA_PAGAMENTO_INDISPONIVEL");
    }
}
