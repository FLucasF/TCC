package com.loja.checkout.service;

import com.loja.checkout.coupon.Bemvindo10;
import com.loja.checkout.coupon.CouponRegistry;
import com.loja.checkout.coupon.FreteGratis;
import com.loja.checkout.coupon.Leve3Pague2;
import com.loja.checkout.coupon.Menos50;
import com.loja.checkout.delivery.DeliveryMethodRegistry;
import com.loja.checkout.delivery.Economica;
import com.loja.checkout.delivery.Expressa;
import com.loja.checkout.delivery.Motoboy;
import com.loja.checkout.delivery.RetiradaLoja;
import com.loja.checkout.dto.ItemRequest;
import com.loja.checkout.dto.ResumoRequest;
import com.loja.checkout.dto.ResumoResponse;
import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.payment.Boleto;
import com.loja.checkout.payment.Cartao;
import com.loja.checkout.payment.PaymentMethodRegistry;
import com.loja.checkout.payment.Pix;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CheckoutServiceTest {

    private CheckoutService service;

    @BeforeEach
    void setUp() {
        DeliveryMethodRegistry deliveryMethodRegistry = new DeliveryMethodRegistry(
                List.of(new Economica(), new Expressa(), new RetiradaLoja(), new Motoboy()));
        CouponRegistry couponRegistry = new CouponRegistry(
                List.of(new Bemvindo10(), new Menos50(), new FreteGratis(), new Leve3Pague2()));
        PaymentMethodRegistry paymentMethodRegistry = new PaymentMethodRegistry(
                List.of(new Pix(), new Cartao(), new Boleto()));
        service = new CheckoutService(deliveryMethodRegistry, couponRegistry, paymentMethodRegistry);
    }

    private ItemRequest camiseta() {
        return new ItemRequest("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30"));
    }

    private ItemRequest tenis() {
        return new ItemRequest("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"));
    }

    @Test
    void exemplo1_expressaComBemvindo10Pix() {
        ResumoRequest request = new ResumoRequest(
                List.of(camiseta(), tenis()), "EXPRESSA", "BEMVINDO10", "PIX", 1, "BRONZE", "NORTE");

        ResumoResponse resposta = service.calcularResumo(request);

        assertThat(resposta.subtotalProdutos()).isEqualByComparingTo("409.70");
        assertThat(resposta.descontoCupom()).isEqualByComparingTo("40.97");
        assertThat(resposta.frete()).isEqualByComparingTo("33.10");
        assertThat(resposta.prazoEntregaDias()).isEqualTo(2);
    }

    @Test
    void exemplo2_economicaSemCupomCartao6x() {
        ResumoRequest request = new ResumoRequest(
                List.of(camiseta(), tenis()), "ECONOMICA", null, "CARTAO", 6, "BRONZE", "NORTE");

        ResumoResponse resposta = service.calcularResumo(request);

        assertThat(resposta.subtotalProdutos()).isEqualByComparingTo("409.70");
        assertThat(resposta.descontoCupom()).isEqualByComparingTo("0.00");
        assertThat(resposta.frete()).isEqualByComparingTo("15.60");
        assertThat(resposta.prazoEntregaDias()).isEqualTo(7);
    }

    @Test
    void exemplo3_motoboyComMenos50Boleto() {
        ItemRequest fone = new ItemRequest("Fone", new BigDecimal("199.90"), 2, new BigDecimal("0.25"));
        ResumoRequest request = new ResumoRequest(
                List.of(fone), "MOTOBOY", "MENOS50", "BOLETO", 1, "BRONZE", "NORTE");

        ResumoResponse resposta = service.calcularResumo(request);

        assertThat(resposta.subtotalProdutos()).isEqualByComparingTo("399.80");
        assertThat(resposta.descontoCupom()).isEqualByComparingTo("50.00");
        assertThat(resposta.frete()).isEqualByComparingTo("18.00");
        assertThat(resposta.prazoEntregaDias()).isEqualTo(0);
        assertThat(resposta.ajustePagamento()).isEqualByComparingTo("3.49");
    }

    @Test
    void exemplo4_retiradaLojaComLeve3Pague2Cartao3x() {
        ItemRequest meia = new ItemRequest("Meia", new BigDecimal("19.90"), 7, new BigDecimal("0.10"));
        ResumoRequest request = new ResumoRequest(
                List.of(meia, camiseta()), "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "BRONZE", "NORTE");

        ResumoResponse resposta = service.calcularResumo(request);

        assertThat(resposta.subtotalProdutos()).isEqualByComparingTo("299.10");
        assertThat(resposta.descontoCupom()).isEqualByComparingTo("39.80");
        assertThat(resposta.frete()).isEqualByComparingTo("0.00");
        assertThat(resposta.prazoEntregaDias()).isEqualTo(1);
        assertThat(resposta.ajustePagamento()).isEqualByComparingTo("0.00");
    }

    @Test
    void exemplo5_expressaSemCupomPixOuroSudeste() {
        ResumoRequest request = new ResumoRequest(
                List.of(camiseta(), tenis()), "EXPRESSA", null, "PIX", 1, "OURO", "SUDESTE");

        ResumoResponse resposta = service.calcularResumo(request);

        assertThat(resposta.subtotalProdutos()).isEqualByComparingTo("409.70");
        assertThat(resposta.descontoCupom()).isEqualByComparingTo("0.00");
        assertThat(resposta.frete()).isEqualByComparingTo("0.00");
        assertThat(resposta.prazoEntregaDias()).isEqualTo(2);
        assertThat(resposta.imposto()).isEqualByComparingTo("49.16");
        assertThat(resposta.ajustePagamento()).isEqualByComparingTo("-22.94");
        assertThat(resposta.totalFinal()).isEqualByComparingTo("435.92");
        assertThat(resposta.valorParcela()).isEqualByComparingTo("435.92");
        assertThat(resposta.creditoProximaCompra()).isEqualByComparingTo("20.48");
        assertThat(resposta.brinde()).isFalse();
    }

    @Test
    void carrinhoVazioDaPedidoInvalido() {
        ResumoRequest request = new ResumoRequest(List.of(), "EXPRESSA", null, "PIX", 1, "BRONZE", "NORTE");

        assertThatThrownBy(() -> service.calcularResumo(request))
                .isInstanceOf(CheckoutException.class)
                .hasMessage("PEDIDO_INVALIDO");
    }

    @Test
    void itemComPrecoZeroDaPedidoInvalido() {
        ItemRequest item = new ItemRequest("X", BigDecimal.ZERO, 1, new BigDecimal("0.1"));
        ResumoRequest request = new ResumoRequest(List.of(item), "EXPRESSA", null, "PIX", 1, "BRONZE", "NORTE");

        assertThatThrownBy(() -> service.calcularResumo(request))
                .isInstanceOf(CheckoutException.class)
                .hasMessage("PEDIDO_INVALIDO");
    }

    @Test
    void nivelClubeInvalido() {
        ResumoRequest request = new ResumoRequest(List.of(camiseta()), "EXPRESSA", null, "PIX", 1, "PLATINA", "NORTE");

        assertThatThrownBy(() -> service.calcularResumo(request))
                .isInstanceOf(CheckoutException.class)
                .hasMessage("NIVEL_CLUBE_INVALIDO");
    }

    @Test
    void regiaoInvalida() {
        ResumoRequest request = new ResumoRequest(List.of(camiseta()), "EXPRESSA", null, "PIX", 1, "BRONZE", "OESTE");

        assertThatThrownBy(() -> service.calcularResumo(request))
                .isInstanceOf(CheckoutException.class)
                .hasMessage("REGIAO_INVALIDA");
    }

    @Test
    void modalidadeInvalida() {
        ResumoRequest request = new ResumoRequest(List.of(camiseta()), "SEDEX", null, "PIX", 1, "BRONZE", "NORTE");

        assertThatThrownBy(() -> service.calcularResumo(request))
                .isInstanceOf(CheckoutException.class)
                .hasMessage("MODALIDADE_INVALIDA");
    }

    @Test
    void motoboyAcimaDoPesoMaximoDaModalidadeIndisponivel() {
        ItemRequest pesado = new ItemRequest("Caixa", new BigDecimal("10.00"), 1, new BigDecimal("6.00"));
        ResumoRequest request = new ResumoRequest(List.of(pesado), "MOTOBOY", null, "PIX", 1, "BRONZE", "NORTE");

        assertThatThrownBy(() -> service.calcularResumo(request))
                .isInstanceOf(CheckoutException.class)
                .hasMessage("MODALIDADE_INDISPONIVEL");
    }

    @Test
    void cupomInvalido() {
        ResumoRequest request = new ResumoRequest(List.of(camiseta()), "EXPRESSA", "NAOEXISTE", "PIX", 1, "BRONZE", "NORTE");

        assertThatThrownBy(() -> service.calcularResumo(request))
                .isInstanceOf(CheckoutException.class)
                .hasMessage("CUPOM_INVALIDO");
    }

    @Test
    void cupomNaoAplicavel() {
        ResumoRequest request = new ResumoRequest(List.of(camiseta()), "EXPRESSA", "MENOS50", "PIX", 1, "BRONZE", "NORTE");

        assertThatThrownBy(() -> service.calcularResumo(request))
                .isInstanceOf(CheckoutException.class)
                .hasMessage("CUPOM_NAO_APLICAVEL");
    }

    @Test
    void formaPagamentoInvalida() {
        ResumoRequest request = new ResumoRequest(List.of(camiseta()), "EXPRESSA", null, "CHEQUE", 1, "BRONZE", "NORTE");

        assertThatThrownBy(() -> service.calcularResumo(request))
                .isInstanceOf(CheckoutException.class)
                .hasMessage("FORMA_PAGAMENTO_INVALIDA");
    }

    @Test
    void parcelamentoInvalidoParaPix() {
        ResumoRequest request = new ResumoRequest(List.of(camiseta()), "EXPRESSA", null, "PIX", 2, "BRONZE", "NORTE");

        assertThatThrownBy(() -> service.calcularResumo(request))
                .isInstanceOf(CheckoutException.class)
                .hasMessage("PARCELAMENTO_INVALIDO");
    }

    @Test
    void parcelamentoInvalidoParaCartaoAcimaDe12() {
        ResumoRequest request = new ResumoRequest(List.of(camiseta()), "EXPRESSA", null, "CARTAO", 13, "BRONZE", "NORTE");

        assertThatThrownBy(() -> service.calcularResumo(request))
                .isInstanceOf(CheckoutException.class)
                .hasMessage("PARCELAMENTO_INVALIDO");
    }

    @Test
    void boletoAcimaDoLimiteDaFormaPagamentoIndisponivel() {
        ItemRequest caro = new ItemRequest("Notebook", new BigDecimal("2000.00"), 1, new BigDecimal("2.00"));
        ResumoRequest request = new ResumoRequest(List.of(caro), "RETIRADA_LOJA", null, "BOLETO", 1, "BRONZE", "NORTE");

        assertThatThrownBy(() -> service.calcularResumo(request))
                .isInstanceOf(CheckoutException.class)
                .hasMessage("FORMA_PAGAMENTO_INDISPONIVEL");
    }
}
