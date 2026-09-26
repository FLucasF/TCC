package com.loja.checkout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.loja.checkout.dto.CheckoutRequest;
import com.loja.checkout.dto.ItemPedido;
import com.loja.checkout.dto.ResumoCompra;
import com.loja.checkout.erro.RegraNegocioException;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class CheckoutServiceTest {

    @Autowired
    private CheckoutService checkoutService;

    @Test
    void exemplo1_expressaComBemvindo10NoPix() {
        CheckoutRequest request = new CheckoutRequest(
                List.of(
                        new ItemPedido("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
                        new ItemPedido("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"))
                ),
                "EXPRESSA", "BEMVINDO10", "PIX", 1
        );

        ResumoCompra resumo = checkoutService.calcularResumo(request);

        assertThat(resumo.subtotalProdutos()).isEqualByComparingTo("409.70");
        assertThat(resumo.descontoCupom()).isEqualByComparingTo("40.97");
        assertThat(resumo.frete()).isEqualByComparingTo("33.10");
        assertThat(resumo.prazoEntregaDias()).isEqualTo(2);
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo("-20.09");
        assertThat(resumo.totalFinal()).isEqualByComparingTo("381.74");
        assertThat(resumo.parcelas()).isEqualTo(1);
        assertThat(resumo.valorParcela()).isEqualByComparingTo("381.74");
    }

    @Test
    void exemplo2_economicaSemCupomCartao6x() {
        CheckoutRequest request = new CheckoutRequest(
                List.of(
                        new ItemPedido("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
                        new ItemPedido("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"))
                ),
                "ECONOMICA", null, "CARTAO", 6
        );

        ResumoCompra resumo = checkoutService.calcularResumo(request);

        assertThat(resumo.subtotalProdutos()).isEqualByComparingTo("409.70");
        assertThat(resumo.descontoCupom()).isEqualByComparingTo("0.00");
        assertThat(resumo.frete()).isEqualByComparingTo("15.60");
        assertThat(resumo.prazoEntregaDias()).isEqualTo(7);
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo("30.10");
        assertThat(resumo.totalFinal()).isEqualByComparingTo("455.40");
        assertThat(resumo.parcelas()).isEqualTo(6);
        assertThat(resumo.valorParcela()).isEqualByComparingTo("75.90");
    }

    @Test
    void exemplo3_motoboyComMenos50Boleto() {
        CheckoutRequest request = new CheckoutRequest(
                List.of(new ItemPedido("Fone", new BigDecimal("199.90"), 2, new BigDecimal("0.25"))),
                "MOTOBOY", "MENOS50", "BOLETO", 1
        );

        ResumoCompra resumo = checkoutService.calcularResumo(request);

        assertThat(resumo.subtotalProdutos()).isEqualByComparingTo("399.80");
        assertThat(resumo.descontoCupom()).isEqualByComparingTo("50.00");
        assertThat(resumo.frete()).isEqualByComparingTo("18.00");
        assertThat(resumo.prazoEntregaDias()).isEqualTo(0);
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo("3.49");
        assertThat(resumo.totalFinal()).isEqualByComparingTo("371.29");
        assertThat(resumo.parcelas()).isEqualTo(1);
        assertThat(resumo.valorParcela()).isEqualByComparingTo("371.29");
    }

    @Test
    void exemplo4_retiradaLojaComLeve3Pague2Cartao3x() {
        CheckoutRequest request = new CheckoutRequest(
                List.of(
                        new ItemPedido("Meia", new BigDecimal("19.90"), 7, new BigDecimal("0.10")),
                        new ItemPedido("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30"))
                ),
                "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3
        );

        ResumoCompra resumo = checkoutService.calcularResumo(request);

        assertThat(resumo.subtotalProdutos()).isEqualByComparingTo("299.10");
        assertThat(resumo.descontoCupom()).isEqualByComparingTo("39.80");
        assertThat(resumo.frete()).isEqualByComparingTo("0.00");
        assertThat(resumo.prazoEntregaDias()).isEqualTo(1);
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo("0.00");
        assertThat(resumo.totalFinal()).isEqualByComparingTo("259.30");
        assertThat(resumo.parcelas()).isEqualTo(3);
        assertThat(resumo.valorParcela()).isEqualByComparingTo("86.43");
    }

    @Test
    void carrinhoVazioRetornaPedidoInvalido() {
        CheckoutRequest request = new CheckoutRequest(List.of(), "EXPRESSA", null, "PIX", 1);

        assertThatThrownBy(() -> checkoutService.calcularResumo(request))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("PEDIDO_INVALIDO");
    }

    @Test
    void itemComQuantidadeZeroRetornaPedidoInvalido() {
        CheckoutRequest request = new CheckoutRequest(
                List.of(new ItemPedido("Camiseta", new BigDecimal("79.90"), 0, new BigDecimal("0.30"))),
                "EXPRESSA", null, "PIX", 1
        );

        assertThatThrownBy(() -> checkoutService.calcularResumo(request))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("PEDIDO_INVALIDO");
    }

    @Test
    void modalidadeInexistenteRetornaModalidadeInvalida() {
        CheckoutRequest request = new CheckoutRequest(
                List.of(new ItemPedido("Camiseta", new BigDecimal("79.90"), 1, new BigDecimal("0.30"))),
                "TELEPORTE", null, "PIX", 1
        );

        assertThatThrownBy(() -> checkoutService.calcularResumo(request))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("MODALIDADE_INVALIDA");
    }

    @Test
    void motoboyAcimaDoLimiteRetornaModalidadeIndisponivel() {
        CheckoutRequest request = new CheckoutRequest(
                List.of(new ItemPedido("Caixa pesada", new BigDecimal("50.00"), 1, new BigDecimal("6"))),
                "MOTOBOY", null, "PIX", 1
        );

        assertThatThrownBy(() -> checkoutService.calcularResumo(request))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("MODALIDADE_INDISPONIVEL");
    }

    @Test
    void cupomInexistenteRetornaCupomInvalido() {
        CheckoutRequest request = new CheckoutRequest(
                List.of(new ItemPedido("Camiseta", new BigDecimal("79.90"), 1, new BigDecimal("0.30"))),
                "EXPRESSA", "NAOEXISTE", "PIX", 1
        );

        assertThatThrownBy(() -> checkoutService.calcularResumo(request))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("CUPOM_INVALIDO");
    }

    @Test
    void menos50AbaixoDoMinimoRetornaCupomNaoAplicavel() {
        CheckoutRequest request = new CheckoutRequest(
                List.of(new ItemPedido("Camiseta", new BigDecimal("79.90"), 1, new BigDecimal("0.30"))),
                "EXPRESSA", "MENOS50", "PIX", 1
        );

        assertThatThrownBy(() -> checkoutService.calcularResumo(request))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("CUPOM_NAO_APLICAVEL");
    }

    @Test
    void formaPagamentoInexistenteRetornaFormaPagamentoInvalida() {
        CheckoutRequest request = new CheckoutRequest(
                List.of(new ItemPedido("Camiseta", new BigDecimal("79.90"), 1, new BigDecimal("0.30"))),
                "EXPRESSA", null, "CHEQUE", 1
        );

        assertThatThrownBy(() -> checkoutService.calcularResumo(request))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("FORMA_PAGAMENTO_INVALIDA");
    }

    @Test
    void pixComMaisDeUmaParcelaRetornaParcelamentoInvalido() {
        CheckoutRequest request = new CheckoutRequest(
                List.of(new ItemPedido("Camiseta", new BigDecimal("79.90"), 1, new BigDecimal("0.30"))),
                "EXPRESSA", null, "PIX", 2
        );

        assertThatThrownBy(() -> checkoutService.calcularResumo(request))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("PARCELAMENTO_INVALIDO");
    }

    @Test
    void cartaoComTrezePercelasRetornaParcelamentoInvalido() {
        CheckoutRequest request = new CheckoutRequest(
                List.of(new ItemPedido("Camiseta", new BigDecimal("79.90"), 1, new BigDecimal("0.30"))),
                "EXPRESSA", null, "CARTAO", 13
        );

        assertThatThrownBy(() -> checkoutService.calcularResumo(request))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("PARCELAMENTO_INVALIDO");
    }

    @Test
    void boletoAcimaDoLimiteRetornaFormaPagamentoIndisponivel() {
        CheckoutRequest request = new CheckoutRequest(
                List.of(new ItemPedido("Notebook", new BigDecimal("1200.00"), 1, new BigDecimal("2"))),
                "RETIRADA_LOJA", null, "BOLETO", 1
        );

        assertThatThrownBy(() -> checkoutService.calcularResumo(request))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("FORMA_PAGAMENTO_INDISPONIVEL");
    }
}
