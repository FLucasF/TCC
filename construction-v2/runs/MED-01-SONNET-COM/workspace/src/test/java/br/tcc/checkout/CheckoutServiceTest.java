package br.tcc.checkout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import br.tcc.checkout.dto.ItemRequest;
import br.tcc.checkout.dto.ResumoRequest;
import br.tcc.checkout.dto.ResumoResponse;

class CheckoutServiceTest {

    private final CheckoutService service = new CheckoutService();

    @Test
    void exemplo1_expressaComBemvindo10EPix() {
        ResumoRequest request = new ResumoRequest(
                List.of(
                        new ItemRequest("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
                        new ItemRequest("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"))),
                "EXPRESSA", "BEMVINDO10", "PIX", 1);

        ResumoResponse resposta = service.calcularResumo(request);

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
    void exemplo2_economicaSemCupomComCartaoEm6x() {
        ResumoRequest request = new ResumoRequest(
                List.of(
                        new ItemRequest("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
                        new ItemRequest("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"))),
                "ECONOMICA", null, "CARTAO", 6);

        ResumoResponse resposta = service.calcularResumo(request);

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
    void exemplo3_motoboyComMenos50EBoleto() {
        ResumoRequest request = new ResumoRequest(
                List.of(new ItemRequest("Fone", new BigDecimal("199.90"), 2, new BigDecimal("0.25"))),
                "MOTOBOY", "MENOS50", "BOLETO", null);

        ResumoResponse resposta = service.calcularResumo(request);

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
    void exemplo4_retiradaLojaComLeve3Pague2ECartaoEm3x() {
        ResumoRequest request = new ResumoRequest(
                List.of(
                        new ItemRequest("Meia", new BigDecimal("19.90"), 7, new BigDecimal("0.10")),
                        new ItemRequest("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30"))),
                "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3);

        ResumoResponse resposta = service.calcularResumo(request);

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
    void carrinhoVazioRetornaPedidoInvalido() {
        ResumoRequest request = new ResumoRequest(List.of(), "EXPRESSA", null, "PIX", 1);

        assertThatThrownBy(() -> service.calcularResumo(request))
                .isInstanceOf(CheckoutException.class)
                .extracting(e -> ((CheckoutException) e).getCodigo())
                .isEqualTo(ErroCodigo.PEDIDO_INVALIDO);
    }

    @Test
    void motoboyAcimaDoLimiteDePesoRetornaModalidadeIndisponivel() {
        ResumoRequest request = new ResumoRequest(
                List.of(new ItemRequest("Caixa", new BigDecimal("10.00"), 1, new BigDecimal("6.00"))),
                "MOTOBOY", null, "PIX", 1);

        assertThatThrownBy(() -> service.calcularResumo(request))
                .isInstanceOf(CheckoutException.class)
                .extracting(e -> ((CheckoutException) e).getCodigo())
                .isEqualTo(ErroCodigo.MODALIDADE_INDISPONIVEL);
    }

    @Test
    void menos50AbaixoDoMinimoRetornaCupomNaoAplicavel() {
        ResumoRequest request = new ResumoRequest(
                List.of(new ItemRequest("Meia", new BigDecimal("19.90"), 1, new BigDecimal("0.10"))),
                "ECONOMICA", "MENOS50", "PIX", 1);

        assertThatThrownBy(() -> service.calcularResumo(request))
                .isInstanceOf(CheckoutException.class)
                .extracting(e -> ((CheckoutException) e).getCodigo())
                .isEqualTo(ErroCodigo.CUPOM_NAO_APLICAVEL);
    }

    @Test
    void boletoAcimaDoLimiteRetornaFormaPagamentoIndisponivel() {
        ResumoRequest request = new ResumoRequest(
                List.of(new ItemRequest("Notebook", new BigDecimal("2000.00"), 1, new BigDecimal("2.00"))),
                "RETIRADA_LOJA", null, "BOLETO", 1);

        assertThatThrownBy(() -> service.calcularResumo(request))
                .isInstanceOf(CheckoutException.class)
                .extracting(e -> ((CheckoutException) e).getCodigo())
                .isEqualTo(ErroCodigo.FORMA_PAGAMENTO_INDISPONIVEL);
    }

    @Test
    void pixComMaisDeUmaParcelaRetornaParcelamentoInvalido() {
        ResumoRequest request = new ResumoRequest(
                List.of(new ItemRequest("Meia", new BigDecimal("19.90"), 1, new BigDecimal("0.10"))),
                "ECONOMICA", null, "PIX", 2);

        assertThatThrownBy(() -> service.calcularResumo(request))
                .isInstanceOf(CheckoutException.class)
                .extracting(e -> ((CheckoutException) e).getCodigo())
                .isEqualTo(ErroCodigo.PARCELAMENTO_INVALIDO);
    }
}
