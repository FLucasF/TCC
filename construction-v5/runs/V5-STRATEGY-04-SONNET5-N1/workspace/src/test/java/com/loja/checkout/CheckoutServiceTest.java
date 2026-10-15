package com.loja.checkout;

import com.loja.checkout.dto.ItemRequest;
import com.loja.checkout.dto.ResumoRequest;
import com.loja.checkout.dto.ResumoResponse;
import com.loja.checkout.erro.CodigoErro;
import com.loja.checkout.erro.ResumoCompraException;
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

    private static ItemRequest camiseta(int quantidade) {
        return new ItemRequest("Camiseta", new BigDecimal("79.90"), quantidade, new BigDecimal("0.30"));
    }

    private static ItemRequest tenis() {
        return new ItemRequest("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"));
    }

    @Test
    void exemplo1_expressaBemvindo10PixBronzeNorte() {
        ResumoRequest requisicao = new ResumoRequest(
                List.of(camiseta(2), tenis()),
                "EXPRESSA", "BEMVINDO10", "PIX", null, "BRONZE", "NORTE");

        ResumoResponse resumo = checkoutService.calcular(requisicao);

        assertThat(resumo.subtotalProdutos()).isEqualByComparingTo("409.70");
        assertThat(resumo.descontoCupom()).isEqualByComparingTo("40.97");
        assertThat(resumo.frete()).isEqualByComparingTo("33.10");
        assertThat(resumo.prazoEntregaDias()).isEqualTo(2);
        assertThat(resumo.seguro()).isEqualByComparingTo("10.24");
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo("-20.60");
        assertThat(resumo.totalFinal()).isEqualByComparingTo("391.47");
        assertThat(resumo.parcelas()).isEqualTo(1);
        assertThat(resumo.valorParcela()).isEqualByComparingTo("391.47");
        assertThat(resumo.creditoProximaCompra()).isEqualByComparingTo("0.00");
        assertThat(resumo.brinde()).isFalse();
    }

    @Test
    void exemplo2_economicaSemCupomCartao6xPrataCentroOeste() {
        ResumoRequest requisicao = new ResumoRequest(
                List.of(camiseta(2), tenis()),
                "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE");

        ResumoResponse resumo = checkoutService.calcular(requisicao);

        assertThat(resumo.subtotalProdutos()).isEqualByComparingTo("409.70");
        assertThat(resumo.descontoCupom()).isEqualByComparingTo("0.00");
        assertThat(resumo.frete()).isEqualByComparingTo("15.60");
        assertThat(resumo.prazoEntregaDias()).isEqualTo(7);
        assertThat(resumo.seguro()).isEqualByComparingTo("6.15");
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo("30.55");
        assertThat(resumo.totalFinal()).isEqualByComparingTo("462.00");
        assertThat(resumo.parcelas()).isEqualTo(6);
        assertThat(resumo.valorParcela()).isEqualByComparingTo("77.00");
        assertThat(resumo.creditoProximaCompra()).isEqualByComparingTo("8.19");
        assertThat(resumo.brinde()).isFalse();
    }

    @Test
    void exemplo3_motoboyMenos50BoletoBronzeNordeste() {
        ResumoRequest requisicao = new ResumoRequest(
                List.of(new ItemRequest("Fone", new BigDecimal("199.90"), 2, new BigDecimal("0.25"))),
                "MOTOBOY", "MENOS50", "BOLETO", null, "BRONZE", "NORDESTE");

        ResumoResponse resumo = checkoutService.calcular(requisicao);

        assertThat(resumo.subtotalProdutos()).isEqualByComparingTo("399.80");
        assertThat(resumo.descontoCupom()).isEqualByComparingTo("50.00");
        assertThat(resumo.frete()).isEqualByComparingTo("18.00");
        assertThat(resumo.prazoEntregaDias()).isEqualTo(0);
        assertThat(resumo.seguro()).isEqualByComparingTo("8.00");
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo("3.49");
        assertThat(resumo.totalFinal()).isEqualByComparingTo("379.29");
        assertThat(resumo.parcelas()).isEqualTo(1);
        assertThat(resumo.valorParcela()).isEqualByComparingTo("379.29");
        assertThat(resumo.creditoProximaCompra()).isEqualByComparingTo("0.00");
        assertThat(resumo.brinde()).isFalse();
    }

    @Test
    void exemplo4_retiradaLojaLeve3Pague2Cartao3xPrataSul() {
        ResumoRequest requisicao = new ResumoRequest(
                List.of(new ItemRequest("Meia", new BigDecimal("19.90"), 7, new BigDecimal("0.10")), camiseta(2)),
                "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL");

        ResumoResponse resumo = checkoutService.calcular(requisicao);

        assertThat(resumo.subtotalProdutos()).isEqualByComparingTo("299.10");
        assertThat(resumo.descontoCupom()).isEqualByComparingTo("39.80");
        assertThat(resumo.frete()).isEqualByComparingTo("0.00");
        assertThat(resumo.prazoEntregaDias()).isEqualTo(1);
        assertThat(resumo.seguro()).isEqualByComparingTo("2.99");
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo("0.00");
        assertThat(resumo.totalFinal()).isEqualByComparingTo("262.29");
        assertThat(resumo.parcelas()).isEqualTo(3);
        assertThat(resumo.valorParcela()).isEqualByComparingTo("87.43");
        assertThat(resumo.creditoProximaCompra()).isEqualByComparingTo("5.98");
        assertThat(resumo.brinde()).isFalse();
    }

    @Test
    void exemplo5_expressaSemCupomPixOuroSudeste() {
        ResumoRequest requisicao = new ResumoRequest(
                List.of(camiseta(2), tenis()),
                "EXPRESSA", null, "PIX", null, "OURO", "SUDESTE");

        ResumoResponse resumo = checkoutService.calcular(requisicao);

        assertThat(resumo.subtotalProdutos()).isEqualByComparingTo("409.70");
        assertThat(resumo.descontoCupom()).isEqualByComparingTo("0.00");
        assertThat(resumo.frete()).isEqualByComparingTo("0.00");
        assertThat(resumo.prazoEntregaDias()).isEqualTo(2);
        assertThat(resumo.seguro()).isEqualByComparingTo("4.10");
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo("-20.69");
        assertThat(resumo.totalFinal()).isEqualByComparingTo("393.11");
        assertThat(resumo.parcelas()).isEqualTo(1);
        assertThat(resumo.valorParcela()).isEqualByComparingTo("393.11");
        assertThat(resumo.creditoProximaCompra()).isEqualByComparingTo("20.48");
        assertThat(resumo.brinde()).isFalse();
    }

    @Test
    void carrinhoVazioDaPedidoInvalido() {
        ResumoRequest requisicao = new ResumoRequest(
                List.of(), "EXPRESSA", null, "PIX", null, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> checkoutService.calcular(requisicao))
                .isInstanceOf(ResumoCompraException.class)
                .extracting(e -> ((ResumoCompraException) e).codigo())
                .isEqualTo(CodigoErro.PEDIDO_INVALIDO);
    }

    @Test
    void itemComQuantidadeZeroDaPedidoInvalido() {
        ResumoRequest requisicao = new ResumoRequest(
                List.of(new ItemRequest("Camiseta", new BigDecimal("79.90"), 0, new BigDecimal("0.30"))),
                "EXPRESSA", null, "PIX", null, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> checkoutService.calcular(requisicao))
                .isInstanceOf(ResumoCompraException.class)
                .extracting(e -> ((ResumoCompraException) e).codigo())
                .isEqualTo(CodigoErro.PEDIDO_INVALIDO);
    }

    @Test
    void nivelClubeInvalido() {
        ResumoRequest requisicao = new ResumoRequest(
                List.of(tenis()), "EXPRESSA", null, "PIX", null, "DIAMANTE", "SUDESTE");

        assertThatThrownBy(() -> checkoutService.calcular(requisicao))
                .isInstanceOf(ResumoCompraException.class)
                .extracting(e -> ((ResumoCompraException) e).codigo())
                .isEqualTo(CodigoErro.NIVEL_CLUBE_INVALIDO);
    }

    @Test
    void regiaoInvalida() {
        ResumoRequest requisicao = new ResumoRequest(
                List.of(tenis()), "EXPRESSA", null, "PIX", null, "BRONZE", "EXTERIOR");

        assertThatThrownBy(() -> checkoutService.calcular(requisicao))
                .isInstanceOf(ResumoCompraException.class)
                .extracting(e -> ((ResumoCompraException) e).codigo())
                .isEqualTo(CodigoErro.REGIAO_INVALIDA);
    }

    @Test
    void modalidadeInvalida() {
        ResumoRequest requisicao = new ResumoRequest(
                List.of(tenis()), "DRONE", null, "PIX", null, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> checkoutService.calcular(requisicao))
                .isInstanceOf(ResumoCompraException.class)
                .extracting(e -> ((ResumoCompraException) e).codigo())
                .isEqualTo(CodigoErro.MODALIDADE_INVALIDA);
    }

    @Test
    void motoboyAcimaDoPesoLimiteDaModalidadeIndisponivel() {
        ResumoRequest requisicao = new ResumoRequest(
                List.of(new ItemRequest("Peso", new BigDecimal("10.00"), 1, new BigDecimal("6.00"))),
                "MOTOBOY", null, "PIX", null, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> checkoutService.calcular(requisicao))
                .isInstanceOf(ResumoCompraException.class)
                .extracting(e -> ((ResumoCompraException) e).codigo())
                .isEqualTo(CodigoErro.MODALIDADE_INDISPONIVEL);
    }

    @Test
    void cupomInvalido() {
        ResumoRequest requisicao = new ResumoRequest(
                List.of(tenis()), "EXPRESSA", "NAOEXISTE", "PIX", null, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> checkoutService.calcular(requisicao))
                .isInstanceOf(ResumoCompraException.class)
                .extracting(e -> ((ResumoCompraException) e).codigo())
                .isEqualTo(CodigoErro.CUPOM_INVALIDO);
    }

    @Test
    void cupomNaoAplicavel() {
        ResumoRequest requisicao = new ResumoRequest(
                List.of(camiseta(1)), "EXPRESSA", "MENOS50", "PIX", null, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> checkoutService.calcular(requisicao))
                .isInstanceOf(ResumoCompraException.class)
                .extracting(e -> ((ResumoCompraException) e).codigo())
                .isEqualTo(CodigoErro.CUPOM_NAO_APLICAVEL);
    }

    @Test
    void formaPagamentoInvalida() {
        ResumoRequest requisicao = new ResumoRequest(
                List.of(tenis()), "EXPRESSA", null, "CRIPTO", null, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> checkoutService.calcular(requisicao))
                .isInstanceOf(ResumoCompraException.class)
                .extracting(e -> ((ResumoCompraException) e).codigo())
                .isEqualTo(CodigoErro.FORMA_PAGAMENTO_INVALIDA);
    }

    @Test
    void parcelamentoInvalidoNoPix() {
        ResumoRequest requisicao = new ResumoRequest(
                List.of(tenis()), "EXPRESSA", null, "PIX", 2, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> checkoutService.calcular(requisicao))
                .isInstanceOf(ResumoCompraException.class)
                .extracting(e -> ((ResumoCompraException) e).codigo())
                .isEqualTo(CodigoErro.PARCELAMENTO_INVALIDO);
    }

    @Test
    void parcelamentoInvalidoNoCartaoAcimaDe12() {
        ResumoRequest requisicao = new ResumoRequest(
                List.of(tenis()), "EXPRESSA", null, "CARTAO", 13, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> checkoutService.calcular(requisicao))
                .isInstanceOf(ResumoCompraException.class)
                .extracting(e -> ((ResumoCompraException) e).codigo())
                .isEqualTo(CodigoErro.PARCELAMENTO_INVALIDO);
    }

    @Test
    void boletoAcimaDeMilIndisponivel() {
        ResumoRequest requisicao = new ResumoRequest(
                List.of(new ItemRequest("Caro", new BigDecimal("2000.00"), 1, new BigDecimal("1.00"))),
                "RETIRADA_LOJA", null, "BOLETO", null, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> checkoutService.calcular(requisicao))
                .isInstanceOf(ResumoCompraException.class)
                .extracting(e -> ((ResumoCompraException) e).codigo())
                .isEqualTo(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
    }

    @Test
    void ouroComProdutosAcimaDe500GanhaBrinde() {
        ResumoRequest requisicao = new ResumoRequest(
                List.of(tenis(), tenis(), tenis()), "RETIRADA_LOJA", null, "PIX", null, "OURO", "SUDESTE");

        ResumoResponse resumo = checkoutService.calcular(requisicao);

        assertThat(resumo.brinde()).isTrue();
    }
}
