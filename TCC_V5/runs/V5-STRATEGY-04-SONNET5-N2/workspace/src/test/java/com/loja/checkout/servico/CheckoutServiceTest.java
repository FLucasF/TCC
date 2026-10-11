package com.loja.checkout.servico;

import com.loja.checkout.dto.ItemRequest;
import com.loja.checkout.dto.ResumoRequest;
import com.loja.checkout.dto.ResumoResponse;
import com.loja.checkout.erro.CheckoutException;
import com.loja.checkout.erro.CodigoErro;
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

    private static final ItemRequest CAMISETA = new ItemRequest("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30"));
    private static final ItemRequest TENIS = new ItemRequest("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"));
    private static final ItemRequest FONE = new ItemRequest("Fone", new BigDecimal("199.90"), 2, new BigDecimal("0.25"));
    private static final ItemRequest MEIA = new ItemRequest("Meia", new BigDecimal("19.90"), 7, new BigDecimal("0.10"));

    @Test
    void exemplo1() {
        ResumoRequest request = new ResumoRequest(
                List.of(CAMISETA, TENIS), "EXPRESSA", "BEMVINDO10", "PIX", null, "BRONZE", "NORTE");

        ResumoResponse resposta = checkoutService.calcular(request);

        assertThat(resposta.subtotalProdutos()).isEqualByComparingTo(new BigDecimal("409.70"));
        assertThat(resposta.descontoCupom()).isEqualByComparingTo(new BigDecimal("40.97"));
        assertThat(resposta.frete()).isEqualByComparingTo(new BigDecimal("33.10"));
        assertThat(resposta.prazoEntregaDias()).isEqualTo(2);
        assertThat(resposta.seguro()).isEqualByComparingTo(new BigDecimal("10.24"));
        assertThat(resposta.ajustePagamento()).isEqualByComparingTo(new BigDecimal("-20.60"));
        assertThat(resposta.totalFinal()).isEqualByComparingTo(new BigDecimal("391.47"));
        assertThat(resposta.parcelas()).isEqualTo(1);
        assertThat(resposta.valorParcela()).isEqualByComparingTo(new BigDecimal("391.47"));
        assertThat(resposta.creditoProximaCompra()).isEqualByComparingTo(new BigDecimal("0.00"));
        assertThat(resposta.brinde()).isFalse();
    }

    @Test
    void exemplo2() {
        ResumoRequest request = new ResumoRequest(
                List.of(CAMISETA, TENIS), "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE");

        ResumoResponse resposta = checkoutService.calcular(request);

        assertThat(resposta.subtotalProdutos()).isEqualByComparingTo(new BigDecimal("409.70"));
        assertThat(resposta.descontoCupom()).isEqualByComparingTo(new BigDecimal("0.00"));
        assertThat(resposta.frete()).isEqualByComparingTo(new BigDecimal("15.60"));
        assertThat(resposta.prazoEntregaDias()).isEqualTo(7);
        assertThat(resposta.seguro()).isEqualByComparingTo(new BigDecimal("6.15"));
        assertThat(resposta.ajustePagamento()).isEqualByComparingTo(new BigDecimal("30.55"));
        assertThat(resposta.totalFinal()).isEqualByComparingTo(new BigDecimal("462.00"));
        assertThat(resposta.parcelas()).isEqualTo(6);
        assertThat(resposta.valorParcela()).isEqualByComparingTo(new BigDecimal("77.00"));
        assertThat(resposta.creditoProximaCompra()).isEqualByComparingTo(new BigDecimal("8.19"));
        assertThat(resposta.brinde()).isFalse();
    }

    @Test
    void exemplo3() {
        ResumoRequest request = new ResumoRequest(
                List.of(FONE), "MOTOBOY", "MENOS50", "BOLETO", null, "BRONZE", "NORDESTE");

        ResumoResponse resposta = checkoutService.calcular(request);

        assertThat(resposta.subtotalProdutos()).isEqualByComparingTo(new BigDecimal("399.80"));
        assertThat(resposta.descontoCupom()).isEqualByComparingTo(new BigDecimal("50.00"));
        assertThat(resposta.frete()).isEqualByComparingTo(new BigDecimal("18.00"));
        assertThat(resposta.prazoEntregaDias()).isEqualTo(0);
        assertThat(resposta.seguro()).isEqualByComparingTo(new BigDecimal("8.00"));
        assertThat(resposta.ajustePagamento()).isEqualByComparingTo(new BigDecimal("3.49"));
        assertThat(resposta.totalFinal()).isEqualByComparingTo(new BigDecimal("379.29"));
        assertThat(resposta.parcelas()).isEqualTo(1);
        assertThat(resposta.valorParcela()).isEqualByComparingTo(new BigDecimal("379.29"));
        assertThat(resposta.creditoProximaCompra()).isEqualByComparingTo(new BigDecimal("0.00"));
        assertThat(resposta.brinde()).isFalse();
    }

    @Test
    void exemplo4() {
        ResumoRequest request = new ResumoRequest(
                List.of(MEIA, CAMISETA), "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL");

        ResumoResponse resposta = checkoutService.calcular(request);

        assertThat(resposta.subtotalProdutos()).isEqualByComparingTo(new BigDecimal("299.10"));
        assertThat(resposta.descontoCupom()).isEqualByComparingTo(new BigDecimal("39.80"));
        assertThat(resposta.frete()).isEqualByComparingTo(new BigDecimal("0.00"));
        assertThat(resposta.prazoEntregaDias()).isEqualTo(1);
        assertThat(resposta.seguro()).isEqualByComparingTo(new BigDecimal("2.99"));
        assertThat(resposta.ajustePagamento()).isEqualByComparingTo(new BigDecimal("0.00"));
        assertThat(resposta.totalFinal()).isEqualByComparingTo(new BigDecimal("262.29"));
        assertThat(resposta.parcelas()).isEqualTo(3);
        assertThat(resposta.valorParcela()).isEqualByComparingTo(new BigDecimal("87.43"));
        assertThat(resposta.creditoProximaCompra()).isEqualByComparingTo(new BigDecimal("5.98"));
        assertThat(resposta.brinde()).isFalse();
    }

    @Test
    void exemplo5() {
        ResumoRequest request = new ResumoRequest(
                List.of(CAMISETA, TENIS), "EXPRESSA", null, "PIX", null, "OURO", "SUDESTE");

        ResumoResponse resposta = checkoutService.calcular(request);

        assertThat(resposta.subtotalProdutos()).isEqualByComparingTo(new BigDecimal("409.70"));
        assertThat(resposta.descontoCupom()).isEqualByComparingTo(new BigDecimal("0.00"));
        assertThat(resposta.frete()).isEqualByComparingTo(new BigDecimal("0.00"));
        assertThat(resposta.prazoEntregaDias()).isEqualTo(2);
        assertThat(resposta.seguro()).isEqualByComparingTo(new BigDecimal("4.10"));
        assertThat(resposta.ajustePagamento()).isEqualByComparingTo(new BigDecimal("-20.69"));
        assertThat(resposta.totalFinal()).isEqualByComparingTo(new BigDecimal("393.11"));
        assertThat(resposta.parcelas()).isEqualTo(1);
        assertThat(resposta.valorParcela()).isEqualByComparingTo(new BigDecimal("393.11"));
        assertThat(resposta.creditoProximaCompra()).isEqualByComparingTo(new BigDecimal("20.48"));
        assertThat(resposta.brinde()).isFalse();
    }

    @Test
    void carrinhoVazioEhInvalido() {
        ResumoRequest request = new ResumoRequest(
                List.of(), "ECONOMICA", null, "PIX", null, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> checkoutService.calcular(request))
                .isInstanceOf(CheckoutException.class)
                .extracting("codigo").isEqualTo(CodigoErro.PEDIDO_INVALIDO);
    }

    @Test
    void nivelClubeInvalidoEhDetectado() {
        ResumoRequest request = new ResumoRequest(
                List.of(CAMISETA), "ECONOMICA", null, "PIX", null, "DIAMANTE", "SUDESTE");

        assertThatThrownBy(() -> checkoutService.calcular(request))
                .isInstanceOf(CheckoutException.class)
                .extracting("codigo").isEqualTo(CodigoErro.NIVEL_CLUBE_INVALIDO);
    }

    @Test
    void regiaoInvalidaEhDetectada() {
        ResumoRequest request = new ResumoRequest(
                List.of(CAMISETA), "ECONOMICA", null, "PIX", null, "BRONZE", "LUA");

        assertThatThrownBy(() -> checkoutService.calcular(request))
                .isInstanceOf(CheckoutException.class)
                .extracting("codigo").isEqualTo(CodigoErro.REGIAO_INVALIDA);
    }

    @Test
    void modalidadeInvalidaEhDetectada() {
        ResumoRequest request = new ResumoRequest(
                List.of(CAMISETA), "TELEPORTE", null, "PIX", null, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> checkoutService.calcular(request))
                .isInstanceOf(CheckoutException.class)
                .extracting("codigo").isEqualTo(CodigoErro.MODALIDADE_INVALIDA);
    }

    @Test
    void motoboyAcimaDoPesoLimiteEhIndisponivel() {
        ItemRequest itemPesado = new ItemRequest("Caixa", new BigDecimal("10.00"), 1, new BigDecimal("6.00"));
        ResumoRequest request = new ResumoRequest(
                List.of(itemPesado), "MOTOBOY", null, "PIX", null, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> checkoutService.calcular(request))
                .isInstanceOf(CheckoutException.class)
                .extracting("codigo").isEqualTo(CodigoErro.MODALIDADE_INDISPONIVEL);
    }

    @Test
    void cupomInvalidoEhDetectado() {
        ResumoRequest request = new ResumoRequest(
                List.of(CAMISETA), "ECONOMICA", "NAOEXISTE", "PIX", null, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> checkoutService.calcular(request))
                .isInstanceOf(CheckoutException.class)
                .extracting("codigo").isEqualTo(CodigoErro.CUPOM_INVALIDO);
    }

    @Test
    void cupomNaoAplicavelEhDetectado() {
        ResumoRequest request = new ResumoRequest(
                List.of(CAMISETA), "ECONOMICA", "MENOS50", "PIX", null, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> checkoutService.calcular(request))
                .isInstanceOf(CheckoutException.class)
                .extracting("codigo").isEqualTo(CodigoErro.CUPOM_NAO_APLICAVEL);
    }

    @Test
    void formaPagamentoInvalidaEhDetectada() {
        ResumoRequest request = new ResumoRequest(
                List.of(CAMISETA), "ECONOMICA", null, "CRIPTO", null, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> checkoutService.calcular(request))
                .isInstanceOf(CheckoutException.class)
                .extracting("codigo").isEqualTo(CodigoErro.FORMA_PAGAMENTO_INVALIDA);
    }

    @Test
    void parcelamentoInvalidoNoPixEhDetectado() {
        ResumoRequest request = new ResumoRequest(
                List.of(CAMISETA), "ECONOMICA", null, "PIX", 2, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> checkoutService.calcular(request))
                .isInstanceOf(CheckoutException.class)
                .extracting("codigo").isEqualTo(CodigoErro.PARCELAMENTO_INVALIDO);
    }

    @Test
    void boletoAcimaDoLimiteEhIndisponivel() {
        ItemRequest itemCaro = new ItemRequest("Notebook", new BigDecimal("2000.00"), 1, new BigDecimal("2.00"));
        ResumoRequest request = new ResumoRequest(
                List.of(itemCaro), "RETIRADA_LOJA", null, "BOLETO", null, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> checkoutService.calcular(request))
                .isInstanceOf(CheckoutException.class)
                .extracting("codigo").isEqualTo(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
    }

    @Test
    void ouroAcimaDoLimiteGanhaBrinde() {
        ItemRequest itemCaro = new ItemRequest("Jaqueta", new BigDecimal("600.00"), 1, new BigDecimal("1.00"));
        ResumoRequest request = new ResumoRequest(
                List.of(itemCaro), "RETIRADA_LOJA", null, "PIX", null, "OURO", "SUDESTE");

        ResumoResponse resposta = checkoutService.calcular(request);

        assertThat(resposta.brinde()).isTrue();
    }
}
