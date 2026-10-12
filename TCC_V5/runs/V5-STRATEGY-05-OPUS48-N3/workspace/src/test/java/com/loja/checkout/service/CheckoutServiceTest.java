package com.loja.checkout.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.loja.checkout.web.CheckoutException;
import com.loja.checkout.web.ResumoRequest;
import com.loja.checkout.web.ResumoRequest.ItemRequest;
import com.loja.checkout.web.ResumoResponse;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class CheckoutServiceTest {

    private final CheckoutService servico = new CheckoutService();

    private static ItemRequest item(String nome, String preco, int qtd, String peso) {
        return new ItemRequest(nome, new BigDecimal(preco), qtd, new BigDecimal(peso));
    }

    private static void assertDinheiro(BigDecimal valor, String esperado) {
        assertThat(valor.toPlainString()).isEqualTo(esperado);
    }

    @Test
    void exemplo1() {
        var req = new ResumoRequest(
                List.of(item("Camiseta", "79.90", 2, "0.30"), item("Tenis", "249.90", 1, "1.20")),
                "EXPRESSA", "BEMVINDO10", "PIX", 1, "BRONZE", "NORTE");

        ResumoResponse r = servico.calcular(req);

        assertDinheiro(r.subtotalProdutos(), "409.70");
        assertDinheiro(r.descontoCupom(), "40.97");
        assertDinheiro(r.frete(), "33.10");
        assertThat(r.prazoEntregaDias()).isEqualTo(2);
        assertDinheiro(r.seguro(), "10.24");
        assertDinheiro(r.ajustePagamento(), "-20.60");
        assertDinheiro(r.totalFinal(), "391.47");
        assertThat(r.parcelas()).isEqualTo(1);
        assertDinheiro(r.valorParcela(), "391.47");
        assertDinheiro(r.creditoProximaCompra(), "0.00");
        assertThat(r.brinde()).isFalse();
    }

    @Test
    void exemplo2() {
        var req = new ResumoRequest(
                List.of(item("Camiseta", "79.90", 2, "0.30"), item("Tenis", "249.90", 1, "1.20")),
                "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE");

        ResumoResponse r = servico.calcular(req);

        assertDinheiro(r.subtotalProdutos(), "409.70");
        assertDinheiro(r.descontoCupom(), "0.00");
        assertDinheiro(r.frete(), "15.60");
        assertThat(r.prazoEntregaDias()).isEqualTo(7);
        assertDinheiro(r.seguro(), "6.15");
        assertDinheiro(r.ajustePagamento(), "30.55");
        assertDinheiro(r.totalFinal(), "462.00");
        assertThat(r.parcelas()).isEqualTo(6);
        assertDinheiro(r.valorParcela(), "77.00");
        assertDinheiro(r.creditoProximaCompra(), "8.19");
        assertThat(r.brinde()).isFalse();
    }

    @Test
    void exemplo3() {
        var req = new ResumoRequest(
                List.of(item("Fone", "199.90", 2, "0.25")),
                "MOTOBOY", "MENOS50", "BOLETO", null, "BRONZE", "NORDESTE");

        ResumoResponse r = servico.calcular(req);

        assertDinheiro(r.subtotalProdutos(), "399.80");
        assertDinheiro(r.descontoCupom(), "50.00");
        assertDinheiro(r.frete(), "18.00");
        assertThat(r.prazoEntregaDias()).isEqualTo(0);
        assertDinheiro(r.seguro(), "8.00");
        assertDinheiro(r.ajustePagamento(), "3.49");
        assertDinheiro(r.totalFinal(), "379.29");
        assertThat(r.parcelas()).isEqualTo(1);
        assertDinheiro(r.valorParcela(), "379.29");
        assertDinheiro(r.creditoProximaCompra(), "0.00");
        assertThat(r.brinde()).isFalse();
    }

    @Test
    void exemplo4() {
        var req = new ResumoRequest(
                List.of(item("Meia", "19.90", 7, "0.10"), item("Camiseta", "79.90", 2, "0.30")),
                "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL");

        ResumoResponse r = servico.calcular(req);

        assertDinheiro(r.subtotalProdutos(), "299.10");
        assertDinheiro(r.descontoCupom(), "39.80");
        assertDinheiro(r.frete(), "0.00");
        assertThat(r.prazoEntregaDias()).isEqualTo(1);
        assertDinheiro(r.seguro(), "2.99");
        assertDinheiro(r.ajustePagamento(), "0.00");
        assertDinheiro(r.totalFinal(), "262.29");
        assertThat(r.parcelas()).isEqualTo(3);
        assertDinheiro(r.valorParcela(), "87.43");
        assertDinheiro(r.creditoProximaCompra(), "5.98");
        assertThat(r.brinde()).isFalse();
    }

    @Test
    void exemplo5() {
        var req = new ResumoRequest(
                List.of(item("Camiseta", "79.90", 2, "0.30"), item("Tenis", "249.90", 1, "1.20")),
                "EXPRESSA", null, "PIX", 1, "OURO", "SUDESTE");

        ResumoResponse r = servico.calcular(req);

        assertDinheiro(r.subtotalProdutos(), "409.70");
        assertDinheiro(r.descontoCupom(), "0.00");
        assertDinheiro(r.frete(), "0.00");
        assertThat(r.prazoEntregaDias()).isEqualTo(2);
        assertDinheiro(r.seguro(), "4.10");
        assertDinheiro(r.ajustePagamento(), "-20.69");
        assertDinheiro(r.totalFinal(), "393.11");
        assertThat(r.parcelas()).isEqualTo(1);
        assertDinheiro(r.valorParcela(), "393.11");
        assertDinheiro(r.creditoProximaCompra(), "20.48");
        assertThat(r.brinde()).isFalse();
    }

    @Test
    void cupomFreteGratisDescontaOValorDoFrete() {
        // EXPRESSA com 1 kg: frete = 25,00 + 4,50 = 29,50; o desconto iguala o frete.
        var req = new ResumoRequest(
                List.of(item("Produto", "100.00", 1, "1.00")),
                "EXPRESSA", "FRETEGRATIS", "PIX", 1, "BRONZE", "SUDESTE");

        ResumoResponse r = servico.calcular(req);

        assertDinheiro(r.frete(), "29.50");
        assertDinheiro(r.descontoCupom(), "29.50");
    }

    @Test
    void boletoNoLimiteDeMilEhAceito() {
        // subtotal 990,10 + seguro 9,90 (1%) = total 1000,00 (exatamente no limite).
        var req = new ResumoRequest(
                List.of(item("Produto", "990.10", 1, "0.50")),
                "RETIRADA_LOJA", null, "BOLETO", 1, "BRONZE", "SUDESTE");

        ResumoResponse r = servico.calcular(req);

        assertDinheiro(r.seguro(), "9.90");
        assertDinheiro(r.totalFinal(), "1003.49");
    }

    @Test
    void brindeOuroAcimaDe500() {
        var req = new ResumoRequest(
                List.of(item("Jaqueta", "600.00", 1, "1.00")),
                "EXPRESSA", null, "PIX", 1, "OURO", "SUDESTE");

        assertThat(servico.calcular(req).brinde()).isTrue();
    }

    private ResumoRequest base(ItemRequest umItem, String modalidade, String cupom,
            String forma, Integer parcelas, String nivel, String regiao) {
        return new ResumoRequest(List.of(umItem), modalidade, cupom, forma, parcelas, nivel, regiao);
    }

    private final ItemRequest ok = item("Produto", "100.00", 1, "0.50");

    private void esperaErro(ResumoRequest req, String codigo) {
        assertThatThrownBy(() -> servico.calcular(req))
                .isInstanceOf(CheckoutException.class)
                .extracting(e -> ((CheckoutException) e).codigo())
                .isEqualTo(codigo);
    }

    @Test
    void carrinhoVazio() {
        esperaErro(new ResumoRequest(List.of(), "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE"),
                "PEDIDO_INVALIDO");
    }

    @Test
    void itemComPrecoZero() {
        esperaErro(base(item("X", "0.00", 1, "0.50"), "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE"),
                "PEDIDO_INVALIDO");
    }

    @Test
    void nivelInvalido() {
        esperaErro(base(ok, "EXPRESSA", null, "PIX", 1, "DIAMANTE", "SUDESTE"),
                "NIVEL_CLUBE_INVALIDO");
    }

    @Test
    void regiaoInvalida() {
        esperaErro(base(ok, "EXPRESSA", null, "PIX", 1, "BRONZE", "LESTE"),
                "REGIAO_INVALIDA");
    }

    @Test
    void modalidadeInvalida() {
        esperaErro(base(ok, "DRONE", null, "PIX", 1, "BRONZE", "SUDESTE"),
                "MODALIDADE_INVALIDA");
    }

    @Test
    void motoboyAcimaDoLimite() {
        esperaErro(base(item("Pesado", "100.00", 1, "6.00"), "MOTOBOY", null, "PIX", 1, "BRONZE", "SUDESTE"),
                "MODALIDADE_INDISPONIVEL");
    }

    @Test
    void cupomInvalido() {
        esperaErro(base(ok, "EXPRESSA", "PROMOX", "PIX", 1, "BRONZE", "SUDESTE"),
                "CUPOM_INVALIDO");
    }

    @Test
    void cupomNaoAplicavel() {
        esperaErro(base(item("Barato", "100.00", 1, "0.50"), "EXPRESSA", "MENOS50", "PIX", 1, "BRONZE", "SUDESTE"),
                "CUPOM_NAO_APLICAVEL");
    }

    @Test
    void formaPagamentoInvalida() {
        esperaErro(base(ok, "EXPRESSA", null, "CRIPTO", 1, "BRONZE", "SUDESTE"),
                "FORMA_PAGAMENTO_INVALIDA");
    }

    @Test
    void parcelamentoInvalido() {
        esperaErro(base(ok, "EXPRESSA", null, "PIX", 2, "BRONZE", "SUDESTE"),
                "PARCELAMENTO_INVALIDO");
    }

    @Test
    void boletoAcimaDeMil() {
        esperaErro(base(item("Caro", "2000.00", 1, "0.50"), "EXPRESSA", null, "BOLETO", 1, "BRONZE", "SUDESTE"),
                "FORMA_PAGAMENTO_INDISPONIVEL");
    }
}
