package com.loja.checkout.service;

import com.loja.checkout.dto.ItemRequest;
import com.loja.checkout.dto.ResumoRequest;
import com.loja.checkout.dto.ResumoResponse;
import com.loja.checkout.exception.CheckoutException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class CheckoutServiceTest {

    @Autowired
    private CheckoutService checkoutService;

    @Test
    void exemplo1_expressaComBemvindo10ePix() {
        ResumoRequest request = new ResumoRequest(
                List.of(
                        new ItemRequest("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
                        new ItemRequest("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"))
                ),
                "EXPRESSA", "BEMVINDO10", "PIX", 1
        );

        ResumoResponse resposta = checkoutService.calcularResumo(request);

        assertThat(resposta.subtotalProdutos()).isEqualByComparingTo("409.70");
        assertThat(resposta.descontoCupom()).isEqualByComparingTo("40.97");
        assertThat(resposta.frete()).isEqualByComparingTo("33.10");
        assertThat(resposta.prazoEntregaDias()).isEqualTo(2);
        assertThat(resposta.ajustePagamento()).isEqualByComparingTo("-20.09");
        assertThat(resposta.totalFinal()).isEqualByComparingTo("381.74");
        assertThat(resposta.parcelas()).isEqualTo(1);
        assertThat(resposta.valorParcela()).isEqualByComparingTo("381.74");
    }

    @Test
    void exemplo2_economicaSemCupomCartao6x() {
        ResumoRequest request = new ResumoRequest(
                List.of(
                        new ItemRequest("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
                        new ItemRequest("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"))
                ),
                "ECONOMICA", null, "CARTAO", 6
        );

        ResumoResponse resposta = checkoutService.calcularResumo(request);

        assertThat(resposta.subtotalProdutos()).isEqualByComparingTo("409.70");
        assertThat(resposta.descontoCupom()).isEqualByComparingTo("0.00");
        assertThat(resposta.frete()).isEqualByComparingTo("15.60");
        assertThat(resposta.prazoEntregaDias()).isEqualTo(7);
        assertThat(resposta.ajustePagamento()).isEqualByComparingTo("30.10");
        assertThat(resposta.totalFinal()).isEqualByComparingTo("455.40");
        assertThat(resposta.parcelas()).isEqualTo(6);
        assertThat(resposta.valorParcela()).isEqualByComparingTo("75.90");
    }

    @Test
    void exemplo3_motoboyComMenos50Boleto() {
        ResumoRequest request = new ResumoRequest(
                List.of(new ItemRequest("Fone", new BigDecimal("199.90"), 2, new BigDecimal("0.25"))),
                "MOTOBOY", "MENOS50", "BOLETO", null
        );

        ResumoResponse resposta = checkoutService.calcularResumo(request);

        assertThat(resposta.subtotalProdutos()).isEqualByComparingTo("399.80");
        assertThat(resposta.descontoCupom()).isEqualByComparingTo("50.00");
        assertThat(resposta.frete()).isEqualByComparingTo("18.00");
        assertThat(resposta.prazoEntregaDias()).isEqualTo(0);
        assertThat(resposta.ajustePagamento()).isEqualByComparingTo("3.49");
        assertThat(resposta.totalFinal()).isEqualByComparingTo("371.29");
        assertThat(resposta.parcelas()).isEqualTo(1);
        assertThat(resposta.valorParcela()).isEqualByComparingTo("371.29");
    }

    @Test
    void exemplo4_retiradaLojaComLeve3Pague2Cartao3x() {
        ResumoRequest request = new ResumoRequest(
                List.of(
                        new ItemRequest("Meia", new BigDecimal("19.90"), 7, new BigDecimal("0.10")),
                        new ItemRequest("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30"))
                ),
                "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3
        );

        ResumoResponse resposta = checkoutService.calcularResumo(request);

        assertThat(resposta.subtotalProdutos()).isEqualByComparingTo("299.10");
        assertThat(resposta.descontoCupom()).isEqualByComparingTo("39.80");
        assertThat(resposta.frete()).isEqualByComparingTo("0.00");
        assertThat(resposta.prazoEntregaDias()).isEqualTo(1);
        assertThat(resposta.ajustePagamento()).isEqualByComparingTo("0.00");
        assertThat(resposta.totalFinal()).isEqualByComparingTo("259.30");
        assertThat(resposta.parcelas()).isEqualTo(3);
        assertThat(resposta.valorParcela()).isEqualByComparingTo("86.43");
    }

    @Test
    void carrinhoVazioDevolvePedidoInvalido() {
        ResumoRequest request = new ResumoRequest(List.of(), "ECONOMICA", null, "PIX", 1);

        assertThatThrownBy(() -> checkoutService.calcularResumo(request))
                .isInstanceOf(CheckoutException.class)
                .hasMessage("PEDIDO_INVALIDO");
    }

    @Test
    void itemComQuantidadeZeroDevolvePedidoInvalido() {
        ResumoRequest request = new ResumoRequest(
                List.of(new ItemRequest("Camiseta", new BigDecimal("79.90"), 0, new BigDecimal("0.30"))),
                "ECONOMICA", null, "PIX", 1
        );

        assertThatThrownBy(() -> checkoutService.calcularResumo(request))
                .isInstanceOf(CheckoutException.class)
                .hasMessage("PEDIDO_INVALIDO");
    }

    @Test
    void modalidadeInexistenteDevolveModalidadeInvalida() {
        ResumoRequest request = new ResumoRequest(
                List.of(new ItemRequest("Camiseta", new BigDecimal("79.90"), 1, new BigDecimal("0.30"))),
                "SEDEX_ESPACIAL", null, "PIX", 1
        );

        assertThatThrownBy(() -> checkoutService.calcularResumo(request))
                .isInstanceOf(CheckoutException.class)
                .hasMessage("MODALIDADE_INVALIDA");
    }

    @Test
    void motoboyAcimaDoPesoMaximoDevolveModalidadeIndisponivel() {
        ResumoRequest request = new ResumoRequest(
                List.of(new ItemRequest("Saco de cimento", new BigDecimal("50.00"), 1, new BigDecimal("6"))),
                "MOTOBOY", null, "PIX", 1
        );

        assertThatThrownBy(() -> checkoutService.calcularResumo(request))
                .isInstanceOf(CheckoutException.class)
                .hasMessage("MODALIDADE_INDISPONIVEL");
    }

    @Test
    void cupomInexistenteDevolveCupomInvalido() {
        ResumoRequest request = new ResumoRequest(
                List.of(new ItemRequest("Camiseta", new BigDecimal("79.90"), 1, new BigDecimal("0.30"))),
                "ECONOMICA", "NAOEXISTE", "PIX", 1
        );

        assertThatThrownBy(() -> checkoutService.calcularResumo(request))
                .isInstanceOf(CheckoutException.class)
                .hasMessage("CUPOM_INVALIDO");
    }

    @Test
    void menos50AbaixoDoMinimoDevolveCupomNaoAplicavel() {
        ResumoRequest request = new ResumoRequest(
                List.of(new ItemRequest("Camiseta", new BigDecimal("79.90"), 1, new BigDecimal("0.30"))),
                "ECONOMICA", "MENOS50", "PIX", 1
        );

        assertThatThrownBy(() -> checkoutService.calcularResumo(request))
                .isInstanceOf(CheckoutException.class)
                .hasMessage("CUPOM_NAO_APLICAVEL");
    }

    @Test
    void formaPagamentoInexistenteDevolveFormaPagamentoInvalida() {
        ResumoRequest request = new ResumoRequest(
                List.of(new ItemRequest("Camiseta", new BigDecimal("79.90"), 1, new BigDecimal("0.30"))),
                "ECONOMICA", null, "CRIPTOMOEDA", 1
        );

        assertThatThrownBy(() -> checkoutService.calcularResumo(request))
                .isInstanceOf(CheckoutException.class)
                .hasMessage("FORMA_PAGAMENTO_INVALIDA");
    }

    @Test
    void pixComParcelasMaiorQueUmDevolveParcelamentoInvalido() {
        ResumoRequest request = new ResumoRequest(
                List.of(new ItemRequest("Camiseta", new BigDecimal("79.90"), 1, new BigDecimal("0.30"))),
                "ECONOMICA", null, "PIX", 2
        );

        assertThatThrownBy(() -> checkoutService.calcularResumo(request))
                .isInstanceOf(CheckoutException.class)
                .hasMessage("PARCELAMENTO_INVALIDO");
    }

    @Test
    void cartaoComTrezePercelasDevolveParcelamentoInvalido() {
        ResumoRequest request = new ResumoRequest(
                List.of(new ItemRequest("Camiseta", new BigDecimal("79.90"), 1, new BigDecimal("0.30"))),
                "ECONOMICA", null, "CARTAO", 13
        );

        assertThatThrownBy(() -> checkoutService.calcularResumo(request))
                .isInstanceOf(CheckoutException.class)
                .hasMessage("PARCELAMENTO_INVALIDO");
    }

    @Test
    void boletoAcimaDoLimiteDevolveFormaPagamentoIndisponivel() {
        ResumoRequest request = new ResumoRequest(
                List.of(new ItemRequest("Notebook", new BigDecimal("1500.00"), 1, new BigDecimal("2"))),
                "RETIRADA_LOJA", null, "BOLETO", 1
        );

        assertThatThrownBy(() -> checkoutService.calcularResumo(request))
                .isInstanceOf(CheckoutException.class)
                .hasMessage("FORMA_PAGAMENTO_INDISPONIVEL");
    }
}
