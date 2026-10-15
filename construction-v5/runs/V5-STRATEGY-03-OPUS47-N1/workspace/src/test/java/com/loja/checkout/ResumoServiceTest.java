package com.loja.checkout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class ResumoServiceTest {

    private final ResumoService service = new ResumoService();

    private static Item camiseta(int qtd) { return new Item("Camiseta", new BigDecimal("79.90"), qtd, new BigDecimal("0.30")); }
    private static Item tenis(int qtd)    { return new Item("Tenis",    new BigDecimal("249.90"), qtd, new BigDecimal("1.20")); }
    private static Item fone(int qtd)     { return new Item("Fone",     new BigDecimal("199.90"), qtd, new BigDecimal("0.25")); }
    private static Item meia(int qtd)     { return new Item("Meia",     new BigDecimal("19.90"), qtd, new BigDecimal("0.10")); }

    @Test void exemplo1() {
        ResumoResponse r = service.calcular(new ResumoRequest(
            List.of(camiseta(2), tenis(1)), "EXPRESSA", "BEMVINDO10", "PIX", 1, "BRONZE", "NORTE"));
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

    @Test void exemplo2() {
        ResumoResponse r = service.calcular(new ResumoRequest(
            List.of(camiseta(2), tenis(1)), "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE"));
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

    @Test void exemplo3() {
        ResumoResponse r = service.calcular(new ResumoRequest(
            List.of(fone(2)), "MOTOBOY", "MENOS50", "BOLETO", 1, "BRONZE", "NORDESTE"));
        assertThat(r.subtotalProdutos()).isEqualByComparingTo("399.80");
        assertThat(r.descontoCupom()).isEqualByComparingTo("50.00");
        assertThat(r.frete()).isEqualByComparingTo("18.00");
        assertThat(r.prazoEntregaDias()).isEqualTo(0);
        assertThat(r.seguro()).isEqualByComparingTo("8.00");
        assertThat(r.ajustePagamento()).isEqualByComparingTo("3.49");
        assertThat(r.totalFinal()).isEqualByComparingTo("379.29");
        assertThat(r.valorParcela()).isEqualByComparingTo("379.29");
        assertThat(r.creditoProximaCompra()).isEqualByComparingTo("0.00");
        assertThat(r.brinde()).isFalse();
    }

    @Test void exemplo4() {
        ResumoResponse r = service.calcular(new ResumoRequest(
            List.of(meia(7), camiseta(2)), "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL"));
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

    @Test void exemplo5() {
        ResumoResponse r = service.calcular(new ResumoRequest(
            List.of(camiseta(2), tenis(1)), "EXPRESSA", null, "PIX", 1, "OURO", "SUDESTE"));
        assertThat(r.subtotalProdutos()).isEqualByComparingTo("409.70");
        assertThat(r.descontoCupom()).isEqualByComparingTo("0.00");
        assertThat(r.frete()).isEqualByComparingTo("0.00");
        assertThat(r.prazoEntregaDias()).isEqualTo(2);
        assertThat(r.seguro()).isEqualByComparingTo("4.10");
        assertThat(r.ajustePagamento()).isEqualByComparingTo("-20.69");
        assertThat(r.totalFinal()).isEqualByComparingTo("393.11");
        assertThat(r.valorParcela()).isEqualByComparingTo("393.11");
        assertThat(r.creditoProximaCompra()).isEqualByComparingTo("20.48");
        assertThat(r.brinde()).isFalse();
    }

    @Test void motoboyAcimaDe5kg() {
        Item pesado = new Item("X", new BigDecimal("10.00"), 1, new BigDecimal("6"));
        assertThatThrownBy(() -> service.calcular(new ResumoRequest(
            List.of(pesado), "MOTOBOY", null, "PIX", 1, "BRONZE", "SUDESTE")))
            .isInstanceOf(ErroPedido.class)
            .hasMessage("MODALIDADE_INDISPONIVEL");
    }

    @Test void menos50AbaixoDe300() {
        assertThatThrownBy(() -> service.calcular(new ResumoRequest(
            List.of(camiseta(1)), "EXPRESSA", "MENOS50", "PIX", 1, "BRONZE", "SUDESTE")))
            .isInstanceOf(ErroPedido.class)
            .hasMessage("CUPOM_NAO_APLICAVEL");
    }

    @Test void boletoAcimaDe1000() {
        Item caro = new Item("X", new BigDecimal("600.00"), 2, new BigDecimal("0.1"));
        assertThatThrownBy(() -> service.calcular(new ResumoRequest(
            List.of(caro), "RETIRADA_LOJA", null, "BOLETO", 1, "BRONZE", "SUDESTE")))
            .isInstanceOf(ErroPedido.class)
            .hasMessage("FORMA_PAGAMENTO_INDISPONIVEL");
    }

    @Test void carrinhoVazio() {
        assertThatThrownBy(() -> service.calcular(new ResumoRequest(
            List.of(), "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE")))
            .isInstanceOf(ErroPedido.class)
            .hasMessage("PEDIDO_INVALIDO");
    }

    @Test void cupomDesconhecido() {
        assertThatThrownBy(() -> service.calcular(new ResumoRequest(
            List.of(camiseta(1)), "EXPRESSA", "XYZ", "PIX", 1, "BRONZE", "SUDESTE")))
            .isInstanceOf(ErroPedido.class)
            .hasMessage("CUPOM_INVALIDO");
    }

    @Test void parcelasInvalidasPix() {
        assertThatThrownBy(() -> service.calcular(new ResumoRequest(
            List.of(camiseta(1)), "EXPRESSA", null, "PIX", 2, "BRONZE", "SUDESTE")))
            .isInstanceOf(ErroPedido.class)
            .hasMessage("PARCELAMENTO_INVALIDO");
    }
}
