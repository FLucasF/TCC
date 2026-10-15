package com.loja.checkout.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.loja.checkout.clube.Bronze;
import com.loja.checkout.clube.Ouro;
import com.loja.checkout.clube.Prata;
import com.loja.checkout.cupom.Bemvindo10;
import com.loja.checkout.cupom.FreteGratis;
import com.loja.checkout.cupom.Leve3Pague2;
import com.loja.checkout.cupom.Menos50;
import com.loja.checkout.entrega.Economica;
import com.loja.checkout.entrega.Expressa;
import com.loja.checkout.entrega.Motoboy;
import com.loja.checkout.entrega.RetiradaLoja;
import com.loja.checkout.pagamento.Boleto;
import com.loja.checkout.pagamento.Cartao;
import com.loja.checkout.pagamento.Pix;
import com.loja.checkout.web.ItemRequest;
import com.loja.checkout.web.ResumoRequest;
import com.loja.checkout.web.ResumoResponse;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class CheckoutServiceTest {

    private final CheckoutService service = new CheckoutService(
            List.of(new Economica(), new Expressa(), new RetiradaLoja(), new Motoboy()),
            List.of(new Bemvindo10(), new Menos50(), new FreteGratis(), new Leve3Pague2()),
            List.of(new Bronze(), new Prata(), new Ouro()),
            List.of(new Pix(), new Cartao(), new Boleto()));

    private static ItemRequest item(String preco, int qtd, String peso) {
        return new ItemRequest("produto", new BigDecimal(preco), qtd, new BigDecimal(peso));
    }

    @Test
    void exemplo1() {
        ResumoResponse r = service.calcular(new ResumoRequest(
                List.of(item("79.90", 2, "0.30"), item("249.90", 1, "1.20")),
                "EXPRESSA", "BEMVINDO10", "PIX", 1, "BRONZE", "NORTE"));

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
        ResumoResponse r = service.calcular(new ResumoRequest(
                List.of(item("79.90", 2, "0.30"), item("249.90", 1, "1.20")),
                "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE"));

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
        ResumoResponse r = service.calcular(new ResumoRequest(
                List.of(item("199.90", 2, "0.25")),
                "MOTOBOY", "MENOS50", "BOLETO", null, "BRONZE", "NORDESTE"));

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
        ResumoResponse r = service.calcular(new ResumoRequest(
                List.of(item("19.90", 7, "0.10"), item("79.90", 2, "0.30")),
                "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL"));

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
        ResumoResponse r = service.calcular(new ResumoRequest(
                List.of(item("79.90", 2, "0.30"), item("249.90", 1, "1.20")),
                "EXPRESSA", null, "PIX", 1, "OURO", "SUDESTE"));

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

    @Test
    void brindeOuroAcimaDe500() {
        ResumoResponse r = service.calcular(new ResumoRequest(
                List.of(item("300.00", 2, "0.50")),
                "RETIRADA_LOJA", null, "PIX", 1, "OURO", "SUDESTE"));

        assertThat(r.brinde()).isTrue();
    }

    @Test
    void todoDinheiroSaiComDuasCasas() {
        ResumoResponse r = service.calcular(new ResumoRequest(
                List.of(item("79.90", 2, "0.30"), item("249.90", 1, "1.20")),
                "EXPRESSA", "BEMVINDO10", "PIX", 1, "BRONZE", "NORTE"));

        assertThat(r.subtotalProdutos().scale()).isEqualTo(2);
        assertThat(r.frete().scale()).isEqualTo(2);
        assertThat(r.creditoProximaCompra().scale()).isEqualTo(2);
        assertThat(r.totalFinal().scale()).isEqualTo(2);
    }

    @Test
    void erroPedidoInvalidoCarrinhoVazio() {
        assertErro(new ResumoRequest(List.of(), "EXPRESSA", null, "PIX", 1, "BRONZE", "NORTE"),
                "PEDIDO_INVALIDO");
    }

    @Test
    void erroPedidoInvalidoItemComPesoZero() {
        assertErro(new ResumoRequest(List.of(item("10.00", 1, "0")), "EXPRESSA", null, "PIX", 1, "BRONZE", "NORTE"),
                "PEDIDO_INVALIDO");
    }

    @Test
    void erroNivelClubeInvalido() {
        assertErro(new ResumoRequest(List.of(item("10.00", 1, "0.10")), "EXPRESSA", null, "PIX", 1, "DIAMANTE", "NORTE"),
                "NIVEL_CLUBE_INVALIDO");
    }

    @Test
    void erroRegiaoInvalida() {
        assertErro(new ResumoRequest(List.of(item("10.00", 1, "0.10")), "EXPRESSA", null, "PIX", 1, "BRONZE", null),
                "REGIAO_INVALIDA");
    }

    @Test
    void erroModalidadeInvalida() {
        assertErro(new ResumoRequest(List.of(item("10.00", 1, "0.10")), "DRONE", null, "PIX", 1, "BRONZE", "NORTE"),
                "MODALIDADE_INVALIDA");
    }

    @Test
    void erroModalidadeIndisponivelMotoboyAcimaDe5kg() {
        assertErro(new ResumoRequest(List.of(item("10.00", 6, "1.00")), "MOTOBOY", null, "PIX", 1, "BRONZE", "NORTE"),
                "MODALIDADE_INDISPONIVEL");
    }

    @Test
    void erroCupomInvalido() {
        assertErro(new ResumoRequest(List.of(item("10.00", 1, "0.10")), "EXPRESSA", "INEXISTENTE", "PIX", 1, "BRONZE", "NORTE"),
                "CUPOM_INVALIDO");
    }

    @Test
    void erroCupomNaoAplicavelMenos50AbaixoDe300() {
        assertErro(new ResumoRequest(List.of(item("100.00", 1, "0.10")), "EXPRESSA", "MENOS50", "PIX", 1, "BRONZE", "NORTE"),
                "CUPOM_NAO_APLICAVEL");
    }

    @Test
    void erroFormaPagamentoInvalida() {
        assertErro(new ResumoRequest(List.of(item("10.00", 1, "0.10")), "EXPRESSA", null, "CRIPTO", 1, "BRONZE", "NORTE"),
                "FORMA_PAGAMENTO_INVALIDA");
    }

    @Test
    void erroParcelamentoInvalidoPixParcelado() {
        assertErro(new ResumoRequest(List.of(item("10.00", 1, "0.10")), "EXPRESSA", null, "PIX", 2, "BRONZE", "NORTE"),
                "PARCELAMENTO_INVALIDO");
    }

    @Test
    void erroParcelamentoInvalidoCartaoAcimaDe12() {
        assertErro(new ResumoRequest(List.of(item("10.00", 1, "0.10")), "EXPRESSA", null, "CARTAO", 13, "BRONZE", "NORTE"),
                "PARCELAMENTO_INVALIDO");
    }

    @Test
    void erroFormaPagamentoIndisponivelBoletoAcimaDe1000() {
        assertErro(new ResumoRequest(List.of(item("600.00", 2, "0.50")), "RETIRADA_LOJA", null, "BOLETO", 1, "BRONZE", "SUDESTE"),
                "FORMA_PAGAMENTO_INDISPONIVEL");
    }

    private void assertErro(ResumoRequest req, String codigo) {
        assertThatThrownBy(() -> service.calcular(req))
                .isInstanceOf(ResumoException.class)
                .extracting("codigo")
                .isEqualTo(codigo);
    }
}
