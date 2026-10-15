package com.loja.checkout.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.loja.checkout.web.ItemRequest;
import com.loja.checkout.web.ResumoRequest;
import com.loja.checkout.web.ResumoResponse;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class CheckoutServiceTest {

    private final CheckoutService service = new CheckoutService();

    private static ItemRequest item(String nome, String preco, int qtd, String peso) {
        return new ItemRequest(nome, new BigDecimal(preco), qtd, new BigDecimal(peso));
    }

    private static BigDecimal reais(String valor) {
        return new BigDecimal(valor);
    }

    private static final List<ItemRequest> CAMISETA_TENIS = List.of(
            item("Camiseta", "79.90", 2, "0.30"),
            item("Tenis", "249.90", 1, "1.20"));

    @Test
    void exemplo1() {
        ResumoResponse r = service.calcular(new ResumoRequest(
                CAMISETA_TENIS, "EXPRESSA", "BEMVINDO10", "PIX", 1, "BRONZE", "NORTE"));

        assertThat(r.subtotalProdutos()).isEqualByComparingTo(reais("409.70"));
        assertThat(r.descontoCupom()).isEqualByComparingTo(reais("40.97"));
        assertThat(r.frete()).isEqualByComparingTo(reais("33.10"));
        assertThat(r.prazoEntregaDias()).isEqualTo(2);
        assertThat(r.seguro()).isEqualByComparingTo(reais("10.24"));
        assertThat(r.ajustePagamento()).isEqualByComparingTo(reais("-20.60"));
        assertThat(r.totalFinal()).isEqualByComparingTo(reais("391.47"));
        assertThat(r.parcelas()).isEqualTo(1);
        assertThat(r.valorParcela()).isEqualByComparingTo(reais("391.47"));
        assertThat(r.creditoProximaCompra()).isEqualByComparingTo(reais("0.00"));
        assertThat(r.brinde()).isFalse();
    }

    @Test
    void exemplo2() {
        ResumoResponse r = service.calcular(new ResumoRequest(
                CAMISETA_TENIS, "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE"));

        assertThat(r.subtotalProdutos()).isEqualByComparingTo(reais("409.70"));
        assertThat(r.descontoCupom()).isEqualByComparingTo(reais("0.00"));
        assertThat(r.frete()).isEqualByComparingTo(reais("15.60"));
        assertThat(r.prazoEntregaDias()).isEqualTo(7);
        assertThat(r.seguro()).isEqualByComparingTo(reais("6.15"));
        assertThat(r.ajustePagamento()).isEqualByComparingTo(reais("30.55"));
        assertThat(r.totalFinal()).isEqualByComparingTo(reais("462.00"));
        assertThat(r.parcelas()).isEqualTo(6);
        assertThat(r.valorParcela()).isEqualByComparingTo(reais("77.00"));
        assertThat(r.creditoProximaCompra()).isEqualByComparingTo(reais("8.19"));
        assertThat(r.brinde()).isFalse();
    }

    @Test
    void exemplo3() {
        ResumoResponse r = service.calcular(new ResumoRequest(
                List.of(item("Fone", "199.90", 2, "0.25")),
                "MOTOBOY", "MENOS50", "BOLETO", null, "BRONZE", "NORDESTE"));

        assertThat(r.subtotalProdutos()).isEqualByComparingTo(reais("399.80"));
        assertThat(r.descontoCupom()).isEqualByComparingTo(reais("50.00"));
        assertThat(r.frete()).isEqualByComparingTo(reais("18.00"));
        assertThat(r.prazoEntregaDias()).isEqualTo(0);
        assertThat(r.seguro()).isEqualByComparingTo(reais("8.00"));
        assertThat(r.ajustePagamento()).isEqualByComparingTo(reais("3.49"));
        assertThat(r.totalFinal()).isEqualByComparingTo(reais("379.29"));
        assertThat(r.parcelas()).isEqualTo(1);
        assertThat(r.valorParcela()).isEqualByComparingTo(reais("379.29"));
        assertThat(r.creditoProximaCompra()).isEqualByComparingTo(reais("0.00"));
        assertThat(r.brinde()).isFalse();
    }

    @Test
    void exemplo4() {
        ResumoResponse r = service.calcular(new ResumoRequest(
                List.of(item("Meia", "19.90", 7, "0.10"), item("Camiseta", "79.90", 2, "0.30")),
                "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL"));

        assertThat(r.subtotalProdutos()).isEqualByComparingTo(reais("299.10"));
        assertThat(r.descontoCupom()).isEqualByComparingTo(reais("39.80"));
        assertThat(r.frete()).isEqualByComparingTo(reais("0.00"));
        assertThat(r.prazoEntregaDias()).isEqualTo(1);
        assertThat(r.seguro()).isEqualByComparingTo(reais("2.99"));
        assertThat(r.ajustePagamento()).isEqualByComparingTo(reais("0.00"));
        assertThat(r.totalFinal()).isEqualByComparingTo(reais("262.29"));
        assertThat(r.parcelas()).isEqualTo(3);
        assertThat(r.valorParcela()).isEqualByComparingTo(reais("87.43"));
        assertThat(r.creditoProximaCompra()).isEqualByComparingTo(reais("5.98"));
        assertThat(r.brinde()).isFalse();
    }

    @Test
    void exemplo5() {
        ResumoResponse r = service.calcular(new ResumoRequest(
                CAMISETA_TENIS, "EXPRESSA", null, "PIX", 1, "OURO", "SUDESTE"));

        assertThat(r.subtotalProdutos()).isEqualByComparingTo(reais("409.70"));
        assertThat(r.descontoCupom()).isEqualByComparingTo(reais("0.00"));
        assertThat(r.frete()).isEqualByComparingTo(reais("0.00"));
        assertThat(r.prazoEntregaDias()).isEqualTo(2);
        assertThat(r.seguro()).isEqualByComparingTo(reais("4.10"));
        assertThat(r.ajustePagamento()).isEqualByComparingTo(reais("-20.69"));
        assertThat(r.totalFinal()).isEqualByComparingTo(reais("393.11"));
        assertThat(r.parcelas()).isEqualTo(1);
        assertThat(r.valorParcela()).isEqualByComparingTo(reais("393.11"));
        assertThat(r.creditoProximaCompra()).isEqualByComparingTo(reais("20.48"));
        assertThat(r.brinde()).isFalse();
    }

    @Test
    void brindeOuroAcimaDe500() {
        ResumoResponse r = service.calcular(new ResumoRequest(
                List.of(item("Casaco", "300.00", 2, "0.50")),
                "RETIRADA_LOJA", null, "PIX", 1, "OURO", "SUDESTE"));
        assertThat(r.brinde()).isTrue();
    }

    private ResumoRequest base(List<ItemRequest> itens, String modalidade, String cupom,
                               String pagamento, Integer parcelas, String nivel, String regiao) {
        return new ResumoRequest(itens, modalidade, cupom, pagamento, parcelas, nivel, regiao);
    }

    private void esperaErro(ResumoRequest req, String codigo) {
        assertThatThrownBy(() -> service.calcular(req))
                .isInstanceOf(PedidoRecusadoException.class)
                .extracting(e -> ((PedidoRecusadoException) e).getCodigo())
                .isEqualTo(codigo);
    }

    @Test
    void carrinhoVazio() {
        esperaErro(base(List.of(), "EXPRESSA", null, "PIX", 1, "BRONZE", "NORTE"), "PEDIDO_INVALIDO");
    }

    @Test
    void itemComPrecoZero() {
        esperaErro(base(List.of(item("X", "0.00", 1, "0.10")), "EXPRESSA", null, "PIX", 1, "BRONZE", "NORTE"),
                "PEDIDO_INVALIDO");
    }

    @Test
    void nivelInvalido() {
        esperaErro(base(CAMISETA_TENIS, "EXPRESSA", null, "PIX", 1, "DIAMANTE", "NORTE"), "NIVEL_CLUBE_INVALIDO");
    }

    @Test
    void regiaoInvalida() {
        esperaErro(base(CAMISETA_TENIS, "EXPRESSA", null, "PIX", 1, "BRONZE", "LESTE"), "REGIAO_INVALIDA");
    }

    @Test
    void modalidadeInvalida() {
        esperaErro(base(CAMISETA_TENIS, "DRONE", null, "PIX", 1, "BRONZE", "NORTE"), "MODALIDADE_INVALIDA");
    }

    @Test
    void motoboyAcimaDe5kg() {
        esperaErro(base(List.of(item("Peso", "10.00", 1, "6.0")), "MOTOBOY", null, "PIX", 1, "BRONZE", "NORTE"),
                "MODALIDADE_INDISPONIVEL");
    }

    @Test
    void cupomInexistente() {
        esperaErro(base(CAMISETA_TENIS, "EXPRESSA", "PROMOX", "PIX", 1, "BRONZE", "NORTE"), "CUPOM_INVALIDO");
    }

    @Test
    void menos50AbaixoDe300() {
        esperaErro(base(List.of(item("Barato", "10.00", 1, "0.10")), "EXPRESSA", "MENOS50", "PIX", 1, "BRONZE", "NORTE"),
                "CUPOM_NAO_APLICAVEL");
    }

    @Test
    void pagamentoInvalido() {
        esperaErro(base(CAMISETA_TENIS, "EXPRESSA", null, "CHEQUE", 1, "BRONZE", "NORTE"), "FORMA_PAGAMENTO_INVALIDA");
    }

    @Test
    void pixParcelado() {
        esperaErro(base(CAMISETA_TENIS, "EXPRESSA", null, "PIX", 3, "BRONZE", "NORTE"), "PARCELAMENTO_INVALIDO");
    }

    @Test
    void cartaoAcimaDe12x() {
        esperaErro(base(CAMISETA_TENIS, "EXPRESSA", null, "CARTAO", 13, "BRONZE", "NORTE"), "PARCELAMENTO_INVALIDO");
    }

    @Test
    void boletoAcimaDe1000() {
        esperaErro(base(List.of(item("Caro", "600.00", 2, "0.50")), "RETIRADA_LOJA", null, "BOLETO", 1, "BRONZE", "SUDESTE"),
                "FORMA_PAGAMENTO_INDISPONIVEL");
    }
}
