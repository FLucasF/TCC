package com.loja.checkout.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.loja.checkout.domain.CheckoutException;
import com.loja.checkout.domain.Resumo;
import com.loja.checkout.web.ItemRequest;
import com.loja.checkout.web.ResumoRequest;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class CalculadoraResumoTest {

    private final CalculadoraResumo calculadora = new CalculadoraResumo();

    private static ItemRequest item(String preco, int qtd, String peso) {
        return new ItemRequest("item", new BigDecimal(preco), qtd, new BigDecimal(peso));
    }

    @Test
    void exemplo1() {
        Resumo r = calculadora.calcular(new ResumoRequest(
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
        Resumo r = calculadora.calcular(new ResumoRequest(
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
        Resumo r = calculadora.calcular(new ResumoRequest(
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
        Resumo r = calculadora.calcular(new ResumoRequest(
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
        Resumo r = calculadora.calcular(new ResumoRequest(
                List.of(item("79.90", 2, "0.30"), item("249.90", 1, "1.20")),
                "EXPRESSA", null, "PIX", 1, "OURO", "SUDESTE"));

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
        Resumo r = calculadora.calcular(new ResumoRequest(
                List.of(item("300.00", 2, "0.30")),
                "RETIRADA_LOJA", null, "PIX", 1, "OURO", "SUDESTE"));
        assertThat(r.brinde()).isTrue();
    }

    @Test
    void brindeOuroExatamente500NaoVai() {
        Resumo r = calculadora.calcular(new ResumoRequest(
                List.of(item("500.00", 1, "0.30")),
                "RETIRADA_LOJA", null, "PIX", 1, "OURO", "SUDESTE"));
        assertThat(r.brinde()).isFalse();
    }

    @Test
    void freteGratisDescontaOValorDoFrete() {
        Resumo r = calculadora.calcular(new ResumoRequest(
                List.of(item("79.90", 2, "0.30"), item("249.90", 1, "1.20")),
                "EXPRESSA", "FRETEGRATIS", "PIX", 1, "BRONZE", "SUDESTE"));
        // frete EXPRESSA cheio = 33,10; desconto do cupom iguala o frete.
        assertThat(r.frete()).isEqualByComparingTo("33.10");
        assertThat(r.descontoCupom()).isEqualByComparingTo("33.10");
    }

    @Test
    void ouroComFreteGratisZeraFreteEDesconto() {
        Resumo r = calculadora.calcular(new ResumoRequest(
                List.of(item("79.90", 2, "0.30"), item("249.90", 1, "1.20")),
                "EXPRESSA", "FRETEGRATIS", "PIX", 1, "OURO", "SUDESTE"));
        // OURO já zera o frete; o cupom iguala esse frete (zero).
        assertThat(r.frete()).isEqualByComparingTo("0.00");
        assertThat(r.descontoCupom()).isEqualByComparingTo("0.00");
    }

    @Test
    void cartaoComJurosMantemValorFinalIgualParcelaVezesParcelas() {
        for (int parcelas : new int[] {4, 12}) {
            Resumo r = calculadora.calcular(new ResumoRequest(
                    List.of(item("79.90", 2, "0.30"), item("249.90", 1, "1.20")),
                    "EXPRESSA", null, "CARTAO", parcelas, "BRONZE", "SUDESTE"));
            assertThat(r.parcelas()).isEqualTo(parcelas);
            assertThat(r.valorParcela().multiply(BigDecimal.valueOf(parcelas)))
                    .isEqualByComparingTo(r.totalFinal());
            // Com juros, o ajuste é positivo (total final acima do total do pedido).
            assertThat(r.ajustePagamento()).isPositive();
        }
    }

    @Test
    void motoboyNoLimiteDe5kgEstaDisponivel() {
        Resumo r = calculadora.calcular(new ResumoRequest(
                List.of(item("50.00", 5, "1.00")),
                "MOTOBOY", null, "PIX", 1, "BRONZE", "SUDESTE"));
        assertThat(r.frete()).isEqualByComparingTo("18.00");
        assertThat(r.prazoEntregaDias()).isEqualTo(0);
    }

    @Test
    void boletoNoLimiteDe1000EstaDisponivel() {
        // RETIRADA_LOJA (frete 0) + SUDESTE 1%: 990,10 + 9,90 = 1000,00 exatos.
        Resumo r = calculadora.calcular(new ResumoRequest(
                List.of(item("990.10", 1, "0.30")),
                "RETIRADA_LOJA", null, "BOLETO", 1, "BRONZE", "SUDESTE"));
        assertThat(r.totalFinal()).isEqualByComparingTo("1003.49");
    }

    @Test
    void pedidoInvalidoItemComPrecoAusente() {
        assertErro(base(List.of(new ItemRequest("item", null, 1, new BigDecimal("0.50"))),
                "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE"), "PEDIDO_INVALIDO");
    }

    private ResumoRequest base(List<ItemRequest> itens, String modalidade, String cupom,
            String pagamento, Integer parcelas, String nivel, String regiao) {
        return new ResumoRequest(itens, modalidade, cupom, pagamento, parcelas, nivel, regiao);
    }

    private final List<ItemRequest> umItem = List.of(item("100.00", 1, "0.50"));

    @Test
    void pedidoInvalidoCarrinhoVazio() {
        assertErro(base(List.of(), "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE"), "PEDIDO_INVALIDO");
    }

    @Test
    void pedidoInvalidoItemComPesoZero() {
        assertErro(base(List.of(item("100.00", 1, "0")), "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE"),
                "PEDIDO_INVALIDO");
    }

    @Test
    void nivelClubeInvalido() {
        assertErro(base(umItem, "EXPRESSA", null, "PIX", 1, "DIAMANTE", "SUDESTE"), "NIVEL_CLUBE_INVALIDO");
    }

    @Test
    void regiaoInvalida() {
        assertErro(base(umItem, "EXPRESSA", null, "PIX", 1, "BRONZE", "LESTE"), "REGIAO_INVALIDA");
    }

    @Test
    void modalidadeInvalida() {
        assertErro(base(umItem, "DRONE", null, "PIX", 1, "BRONZE", "SUDESTE"), "MODALIDADE_INVALIDA");
    }

    @Test
    void modalidadeIndisponivelMotoboyAcimaDe5kg() {
        assertErro(base(List.of(item("100.00", 6, "1.00")), "MOTOBOY", null, "PIX", 1, "BRONZE", "SUDESTE"),
                "MODALIDADE_INDISPONIVEL");
    }

    @Test
    void cupomInvalido() {
        assertErro(base(umItem, "EXPRESSA", "INEXISTENTE", "PIX", 1, "BRONZE", "SUDESTE"), "CUPOM_INVALIDO");
    }

    @Test
    void cupomNaoAplicavelMenos50AbaixoDe300() {
        assertErro(base(umItem, "EXPRESSA", "MENOS50", "PIX", 1, "BRONZE", "SUDESTE"), "CUPOM_NAO_APLICAVEL");
    }

    @Test
    void formaPagamentoInvalida() {
        assertErro(base(umItem, "EXPRESSA", null, "CRIPTO", 1, "BRONZE", "SUDESTE"), "FORMA_PAGAMENTO_INVALIDA");
    }

    @Test
    void parcelamentoInvalidoPixParcelado() {
        assertErro(base(umItem, "EXPRESSA", null, "PIX", 2, "BRONZE", "SUDESTE"), "PARCELAMENTO_INVALIDO");
    }

    @Test
    void parcelamentoInvalidoCartaoAcimaDe12() {
        assertErro(base(umItem, "EXPRESSA", null, "CARTAO", 13, "BRONZE", "SUDESTE"), "PARCELAMENTO_INVALIDO");
    }

    @Test
    void formaPagamentoIndisponivelBoletoAcimaDe1000() {
        assertErro(base(List.of(item("600.00", 2, "0.50")), "EXPRESSA", null, "BOLETO", 1, "BRONZE", "SUDESTE"),
                "FORMA_PAGAMENTO_INDISPONIVEL");
    }

    private void assertErro(ResumoRequest req, String codigo) {
        assertThatThrownBy(() -> calculadora.calcular(req))
                .isInstanceOf(CheckoutException.class)
                .hasMessage(codigo);
    }
}
