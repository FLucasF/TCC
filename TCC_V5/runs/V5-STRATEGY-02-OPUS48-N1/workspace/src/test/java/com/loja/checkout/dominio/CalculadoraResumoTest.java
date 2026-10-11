package com.loja.checkout.dominio;

import com.loja.checkout.web.ItemRequest;
import com.loja.checkout.web.ResumoRequest;
import com.loja.checkout.web.ResumoResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CalculadoraResumoTest {

    private final CalculadoraResumo calculadora = new CalculadoraResumo();

    private static ItemRequest item(String nome, String preco, int qtd, String peso) {
        return new ItemRequest(nome, new BigDecimal(preco), qtd, new BigDecimal(peso));
    }

    private static BigDecimal reais(String v) {
        return new BigDecimal(v);
    }

    private void recusa(ResumoRequest req, String codigo) {
        assertThatThrownBy(() -> calculadora.calcular(req))
                .isInstanceOf(CheckoutException.class)
                .extracting("codigo")
                .isEqualTo(codigo);
    }

    @Test
    void exemplo1() {
        var req = new ResumoRequest(
                List.of(item("Camiseta", "79.90", 2, "0.30"), item("Tenis", "249.90", 1, "1.20")),
                "EXPRESSA", "BEMVINDO10", "PIX", 1, "BRONZE", "NORTE");

        ResumoResponse r = calculadora.calcular(req);

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
        var req = new ResumoRequest(
                List.of(item("Camiseta", "79.90", 2, "0.30"), item("Tenis", "249.90", 1, "1.20")),
                "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE");

        ResumoResponse r = calculadora.calcular(req);

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
        var req = new ResumoRequest(
                List.of(item("Fone", "199.90", 2, "0.25")),
                "MOTOBOY", "MENOS50", "BOLETO", 1, "BRONZE", "NORDESTE");

        ResumoResponse r = calculadora.calcular(req);

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
        var req = new ResumoRequest(
                List.of(item("Meia", "19.90", 7, "0.10"), item("Camiseta", "79.90", 2, "0.30")),
                "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL");

        ResumoResponse r = calculadora.calcular(req);

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
        var req = new ResumoRequest(
                List.of(item("Camiseta", "79.90", 2, "0.30"), item("Tenis", "249.90", 1, "1.20")),
                "EXPRESSA", null, "PIX", 1, "OURO", "SUDESTE");

        ResumoResponse r = calculadora.calcular(req);

        assertThat(r.subtotalProdutos()).isEqualByComparingTo(reais("409.70"));
        assertThat(r.descontoCupom()).isEqualByComparingTo(reais("0.00"));
        assertThat(r.frete()).isEqualByComparingTo(reais("0.00"));
        assertThat(r.prazoEntregaDias()).isEqualTo(2);
        assertThat(r.seguro()).isEqualByComparingTo(reais("4.10"));
        assertThat(r.ajustePagamento()).isEqualByComparingTo(reais("-20.69"));
        assertThat(r.totalFinal()).isEqualByComparingTo(reais("393.11"));
        assertThat(r.valorParcela()).isEqualByComparingTo(reais("393.11"));
        assertThat(r.creditoProximaCompra()).isEqualByComparingTo(reais("20.48"));
        assertThat(r.brinde()).isFalse();
    }

    @Test
    void exemploDoAnexo() {
        var req = new ResumoRequest(
                List.of(item("Camiseta", "79.90", 2, "0.30"), item("Tenis", "249.90", 1, "1.20")),
                "EXPRESSA", "BEMVINDO10", "PIX", 1, "OURO", "SUDESTE");

        ResumoResponse r = calculadora.calcular(req);

        assertThat(r.subtotalProdutos()).isEqualByComparingTo(reais("409.70"));
        assertThat(r.descontoCupom()).isEqualByComparingTo(reais("40.97"));
        assertThat(r.frete()).isEqualByComparingTo(reais("0.00"));
        assertThat(r.prazoEntregaDias()).isEqualTo(2);
        assertThat(r.seguro()).isEqualByComparingTo(reais("4.10"));
        assertThat(r.ajustePagamento()).isEqualByComparingTo(reais("-18.64"));
        assertThat(r.totalFinal()).isEqualByComparingTo(reais("354.19"));
        assertThat(r.valorParcela()).isEqualByComparingTo(reais("354.19"));
        assertThat(r.creditoProximaCompra()).isEqualByComparingTo(reais("20.48"));
        assertThat(r.brinde()).isFalse();
    }

    @Test
    void brindeOuroAcimaDe500() {
        var req = new ResumoRequest(
                List.of(item("Casaco", "300.00", 2, "0.80")),
                "RETIRADA_LOJA", null, "PIX", 1, "OURO", "SUDESTE");

        ResumoResponse r = calculadora.calcular(req);

        assertThat(r.subtotalProdutos()).isEqualByComparingTo(reais("600.00"));
        assertThat(r.brinde()).isTrue();
    }

    @Test
    void parcelasAusentesContamComoUma() {
        var req = new ResumoRequest(
                List.of(item("Fone", "199.90", 1, "0.25")),
                "RETIRADA_LOJA", null, "CARTAO", null, "BRONZE", "SUDESTE");

        ResumoResponse r = calculadora.calcular(req);

        assertThat(r.parcelas()).isEqualTo(1);
    }

    @Test
    void fretegratisDescontaOValorDoFrete() {
        var req = new ResumoRequest(
                List.of(item("Fone", "199.90", 1, "0.25")),
                "EXPRESSA", "FRETEGRATIS", "PIX", 1, "BRONZE", "SUDESTE");

        ResumoResponse r = calculadora.calcular(req);

        assertThat(r.frete()).isEqualByComparingTo(reais("26.12")); // 25 + 4.50 * 0.25 = 26.125 -> meio para o par
        assertThat(r.descontoCupom()).isEqualByComparingTo(r.frete());
    }

    @Test
    void carrinhoVazio() {
        recusa(new ResumoRequest(List.of(), "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE"),
                "PEDIDO_INVALIDO");
    }

    @Test
    void itemComQuantidadeZero() {
        recusa(new ResumoRequest(List.of(item("X", "10.00", 0, "0.10")),
                "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE"), "PEDIDO_INVALIDO");
    }

    @Test
    void itemComPrecoAusente() {
        var itemSemPreco = new ItemRequest("X", null, 1, new BigDecimal("0.10"));
        recusa(new ResumoRequest(List.of(itemSemPreco),
                "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE"), "PEDIDO_INVALIDO");
    }

    @Test
    void nivelClubeInvalido() {
        recusa(new ResumoRequest(List.of(item("X", "10.00", 1, "0.10")),
                "EXPRESSA", null, "PIX", 1, "DIAMANTE", "SUDESTE"), "NIVEL_CLUBE_INVALIDO");
    }

    @Test
    void regiaoInvalida() {
        recusa(new ResumoRequest(List.of(item("X", "10.00", 1, "0.10")),
                "EXPRESSA", null, "PIX", 1, "BRONZE", "LESTE"), "REGIAO_INVALIDA");
    }

    @Test
    void modalidadeInvalida() {
        recusa(new ResumoRequest(List.of(item("X", "10.00", 1, "0.10")),
                "DRONE", null, "PIX", 1, "BRONZE", "SUDESTE"), "MODALIDADE_INVALIDA");
    }

    @Test
    void motoboyAcimaDe5kg() {
        recusa(new ResumoRequest(List.of(item("Halter", "100.00", 1, "6.00")),
                "MOTOBOY", null, "PIX", 1, "BRONZE", "SUDESTE"), "MODALIDADE_INDISPONIVEL");
    }

    @Test
    void cupomInvalido() {
        recusa(new ResumoRequest(List.of(item("X", "10.00", 1, "0.10")),
                "EXPRESSA", "NATAL2030", "PIX", 1, "BRONZE", "SUDESTE"), "CUPOM_INVALIDO");
    }

    @Test
    void menos50AbaixoDoMinimo() {
        recusa(new ResumoRequest(List.of(item("X", "100.00", 1, "0.10")),
                "EXPRESSA", "MENOS50", "PIX", 1, "BRONZE", "SUDESTE"), "CUPOM_NAO_APLICAVEL");
    }

    @Test
    void formaPagamentoInvalida() {
        recusa(new ResumoRequest(List.of(item("X", "10.00", 1, "0.10")),
                "EXPRESSA", null, "DINHEIRO", 1, "BRONZE", "SUDESTE"), "FORMA_PAGAMENTO_INVALIDA");
    }

    @Test
    void pixNaoParcela() {
        recusa(new ResumoRequest(List.of(item("X", "10.00", 1, "0.10")),
                "EXPRESSA", null, "PIX", 2, "BRONZE", "SUDESTE"), "PARCELAMENTO_INVALIDO");
    }

    @Test
    void cartaoAcimaDe12x() {
        recusa(new ResumoRequest(List.of(item("X", "10.00", 1, "0.10")),
                "EXPRESSA", null, "CARTAO", 13, "BRONZE", "SUDESTE"), "PARCELAMENTO_INVALIDO");
    }

    @Test
    void boletoAcimaDe1000() {
        recusa(new ResumoRequest(List.of(item("TV", "2000.00", 1, "3.00")),
                "RETIRADA_LOJA", null, "BOLETO", 1, "BRONZE", "SUDESTE"), "FORMA_PAGAMENTO_INDISPONIVEL");
    }
}
