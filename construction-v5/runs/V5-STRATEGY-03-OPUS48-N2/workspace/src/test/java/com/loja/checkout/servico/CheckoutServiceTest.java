package com.loja.checkout.servico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.loja.checkout.erro.CheckoutException;
import com.loja.checkout.erro.CodigoErro;
import com.loja.checkout.web.ItemRequest;
import com.loja.checkout.web.ResumoRequest;
import com.loja.checkout.web.ResumoResponse;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class CheckoutServiceTest {

    @Autowired
    private CheckoutService servico;

    private static final ItemRequest CAMISETA = new ItemRequest("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30"));
    private static final ItemRequest TENIS = new ItemRequest("Tenis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"));

    @Test
    void exemplo1() {
        ResumoResponse r = servico.calcular(new ResumoRequest(
                List.of(CAMISETA, TENIS), "EXPRESSA", "BEMVINDO10", "PIX", 1, "BRONZE", "NORTE"));

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
        ResumoResponse r = servico.calcular(new ResumoRequest(
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
        ResumoResponse r = servico.calcular(new ResumoRequest(
                List.of(new ItemRequest("Fone", new BigDecimal("199.90"), 2, new BigDecimal("0.25"))),
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
        ResumoResponse r = servico.calcular(new ResumoRequest(
                List.of(new ItemRequest("Meia", new BigDecimal("19.90"), 7, new BigDecimal("0.10")), CAMISETA),
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
        ResumoResponse r = servico.calcular(new ResumoRequest(
                List.of(CAMISETA, TENIS), "EXPRESSA", null, "PIX", 1, "OURO", "SUDESTE"));

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
        ResumoResponse r = servico.calcular(new ResumoRequest(
                List.of(new ItemRequest("Tenis", new BigDecimal("300.00"), 2, new BigDecimal("1.00"))),
                "RETIRADA_LOJA", null, "PIX", 1, "OURO", "SUDESTE"));

        assertThat(r.subtotalProdutos()).isEqualByComparingTo("600.00");
        assertThat(r.brinde()).isTrue();
    }

    @Test
    void carrinhoVazioRecusado() {
        assertErro(new ResumoRequest(List.of(), "EXPRESSA", null, "PIX", 1, "BRONZE", "NORTE"),
                CodigoErro.PEDIDO_INVALIDO);
    }

    @Test
    void itemComPrecoZeroRecusado() {
        assertErro(new ResumoRequest(
                        List.of(new ItemRequest("X", BigDecimal.ZERO, 1, new BigDecimal("0.10"))),
                        "EXPRESSA", null, "PIX", 1, "BRONZE", "NORTE"),
                CodigoErro.PEDIDO_INVALIDO);
    }

    @Test
    void nivelClubeInvalidoRecusado() {
        assertErro(new ResumoRequest(List.of(CAMISETA), "EXPRESSA", null, "PIX", 1, "DIAMANTE", "NORTE"),
                CodigoErro.NIVEL_CLUBE_INVALIDO);
    }

    @Test
    void regiaoInvalidaRecusada() {
        assertErro(new ResumoRequest(List.of(CAMISETA), "EXPRESSA", null, "PIX", 1, "BRONZE", "LESTE"),
                CodigoErro.REGIAO_INVALIDA);
    }

    @Test
    void modalidadeInvalidaRecusada() {
        assertErro(new ResumoRequest(List.of(CAMISETA), "DRONE", null, "PIX", 1, "BRONZE", "NORTE"),
                CodigoErro.MODALIDADE_INVALIDA);
    }

    @Test
    void motoboyAcimaDe5kgIndisponivel() {
        assertErro(new ResumoRequest(
                        List.of(new ItemRequest("Peso", new BigDecimal("10.00"), 1, new BigDecimal("6.00"))),
                        "MOTOBOY", null, "PIX", 1, "BRONZE", "NORTE"),
                CodigoErro.MODALIDADE_INDISPONIVEL);
    }

    @Test
    void cupomInexistenteRecusado() {
        assertErro(new ResumoRequest(List.of(CAMISETA), "EXPRESSA", "INEXISTENTE", "PIX", 1, "BRONZE", "NORTE"),
                CodigoErro.CUPOM_INVALIDO);
    }

    @Test
    void menos50AbaixoDoMinimoNaoAplicavel() {
        assertErro(new ResumoRequest(
                        List.of(new ItemRequest("Meia", new BigDecimal("19.90"), 1, new BigDecimal("0.10"))),
                        "EXPRESSA", "MENOS50", "PIX", 1, "BRONZE", "NORTE"),
                CodigoErro.CUPOM_NAO_APLICAVEL);
    }

    @Test
    void formaPagamentoInvalidaRecusada() {
        assertErro(new ResumoRequest(List.of(CAMISETA), "EXPRESSA", null, "CHEQUE", 1, "BRONZE", "NORTE"),
                CodigoErro.FORMA_PAGAMENTO_INVALIDA);
    }

    @Test
    void pixParceladoRecusado() {
        assertErro(new ResumoRequest(List.of(CAMISETA), "EXPRESSA", null, "PIX", 2, "BRONZE", "NORTE"),
                CodigoErro.PARCELAMENTO_INVALIDO);
    }

    @Test
    void cartaoAcimaDe12ParcelasRecusado() {
        assertErro(new ResumoRequest(List.of(CAMISETA), "EXPRESSA", null, "CARTAO", 13, "BRONZE", "NORTE"),
                CodigoErro.PARCELAMENTO_INVALIDO);
    }

    @Test
    void boletoAcimaDe1000Indisponivel() {
        assertErro(new ResumoRequest(
                        List.of(new ItemRequest("Movel", new BigDecimal("600.00"), 2, new BigDecimal("1.00"))),
                        "RETIRADA_LOJA", null, "BOLETO", 1, "BRONZE", "SUDESTE"),
                CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
    }

    private void assertErro(ResumoRequest req, CodigoErro esperado) {
        assertThatThrownBy(() -> servico.calcular(req))
                .isInstanceOf(CheckoutException.class)
                .extracting(e -> ((CheckoutException) e).codigo())
                .isEqualTo(esperado);
    }
}
