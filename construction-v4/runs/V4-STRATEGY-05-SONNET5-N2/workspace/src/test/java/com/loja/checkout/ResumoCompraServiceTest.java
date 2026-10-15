package com.loja.checkout;

import com.loja.checkout.dto.ItemPedidoRequest;
import com.loja.checkout.dto.ResumoRequest;
import com.loja.checkout.dto.ResumoResponse;
import com.loja.checkout.erro.CheckoutException;
import com.loja.checkout.erro.ErroCheckout;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class ResumoCompraServiceTest {

    @Autowired
    private ResumoCompraService service;

    private static ItemPedidoRequest camiseta() {
        return new ItemPedidoRequest("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30"));
    }

    private static ItemPedidoRequest tenis() {
        return new ItemPedidoRequest("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"));
    }

    @Test
    void exemplo1_expressaBemvindo10PixBronzeNorte() {
        ResumoRequest request = new ResumoRequest(
                List.of(camiseta(), tenis()), "EXPRESSA", "BEMVINDO10", "PIX", null, "BRONZE", "NORTE");

        ResumoResponse resposta = service.calcular(request);

        assertThat(resposta.subtotalProdutos()).isEqualByComparingTo("409.70");
        assertThat(resposta.descontoCupom()).isEqualByComparingTo("40.97");
        assertThat(resposta.frete()).isEqualByComparingTo("33.10");
        assertThat(resposta.prazoEntregaDias()).isEqualTo(2);
        assertThat(resposta.seguro()).isEqualByComparingTo("10.24");
        assertThat(resposta.ajustePagamento()).isEqualByComparingTo("-20.60");
        assertThat(resposta.totalFinal()).isEqualByComparingTo("391.47");
        assertThat(resposta.parcelas()).isEqualTo(1);
        assertThat(resposta.valorParcela()).isEqualByComparingTo("391.47");
        assertThat(resposta.creditoProximaCompra()).isEqualByComparingTo("0.00");
        assertThat(resposta.brinde()).isFalse();
    }

    @Test
    void exemplo2_economicaSemCupomCartao6xPrataCentroOeste() {
        ResumoRequest request = new ResumoRequest(
                List.of(camiseta(), tenis()), "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE");

        ResumoResponse resposta = service.calcular(request);

        assertThat(resposta.subtotalProdutos()).isEqualByComparingTo("409.70");
        assertThat(resposta.descontoCupom()).isEqualByComparingTo("0.00");
        assertThat(resposta.frete()).isEqualByComparingTo("15.60");
        assertThat(resposta.prazoEntregaDias()).isEqualTo(7);
        assertThat(resposta.seguro()).isEqualByComparingTo("6.15");
        assertThat(resposta.ajustePagamento()).isEqualByComparingTo("30.55");
        assertThat(resposta.totalFinal()).isEqualByComparingTo("462.00");
        assertThat(resposta.parcelas()).isEqualTo(6);
        assertThat(resposta.valorParcela()).isEqualByComparingTo("77.00");
        assertThat(resposta.creditoProximaCompra()).isEqualByComparingTo("8.19");
        assertThat(resposta.brinde()).isFalse();
    }

    @Test
    void exemplo3_motoboyMenos50BoletoBronzeNordeste() {
        ItemPedidoRequest fone = new ItemPedidoRequest("Fone", new BigDecimal("199.90"), 2, new BigDecimal("0.25"));
        ResumoRequest request = new ResumoRequest(
                List.of(fone), "MOTOBOY", "MENOS50", "BOLETO", null, "BRONZE", "NORDESTE");

        ResumoResponse resposta = service.calcular(request);

        assertThat(resposta.subtotalProdutos()).isEqualByComparingTo("399.80");
        assertThat(resposta.descontoCupom()).isEqualByComparingTo("50.00");
        assertThat(resposta.frete()).isEqualByComparingTo("18.00");
        assertThat(resposta.prazoEntregaDias()).isEqualTo(0);
        assertThat(resposta.seguro()).isEqualByComparingTo("8.00");
        assertThat(resposta.ajustePagamento()).isEqualByComparingTo("3.49");
        assertThat(resposta.totalFinal()).isEqualByComparingTo("379.29");
        assertThat(resposta.parcelas()).isEqualTo(1);
        assertThat(resposta.valorParcela()).isEqualByComparingTo("379.29");
        assertThat(resposta.creditoProximaCompra()).isEqualByComparingTo("0.00");
        assertThat(resposta.brinde()).isFalse();
    }

    @Test
    void exemplo4_retiradaLoja3Pague2Cartao3xPrataSul() {
        ItemPedidoRequest meia = new ItemPedidoRequest("Meia", new BigDecimal("19.90"), 7, new BigDecimal("0.10"));
        ResumoRequest request = new ResumoRequest(
                List.of(meia, camiseta()), "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL");

        ResumoResponse resposta = service.calcular(request);

        assertThat(resposta.subtotalProdutos()).isEqualByComparingTo("299.10");
        assertThat(resposta.descontoCupom()).isEqualByComparingTo("39.80");
        assertThat(resposta.frete()).isEqualByComparingTo("0.00");
        assertThat(resposta.prazoEntregaDias()).isEqualTo(1);
        assertThat(resposta.seguro()).isEqualByComparingTo("2.99");
        assertThat(resposta.ajustePagamento()).isEqualByComparingTo("0.00");
        assertThat(resposta.totalFinal()).isEqualByComparingTo("262.29");
        assertThat(resposta.parcelas()).isEqualTo(3);
        assertThat(resposta.valorParcela()).isEqualByComparingTo("87.43");
        assertThat(resposta.creditoProximaCompra()).isEqualByComparingTo("5.98");
        assertThat(resposta.brinde()).isFalse();
    }

    @Test
    void exemplo5_expressaSemCupomPixOuroSudeste() {
        ResumoRequest request = new ResumoRequest(
                List.of(camiseta(), tenis()), "EXPRESSA", null, "PIX", null, "OURO", "SUDESTE");

        ResumoResponse resposta = service.calcular(request);

        assertThat(resposta.subtotalProdutos()).isEqualByComparingTo("409.70");
        assertThat(resposta.descontoCupom()).isEqualByComparingTo("0.00");
        assertThat(resposta.frete()).isEqualByComparingTo("0.00");
        assertThat(resposta.prazoEntregaDias()).isEqualTo(2);
        assertThat(resposta.seguro()).isEqualByComparingTo("4.10");
        assertThat(resposta.ajustePagamento()).isEqualByComparingTo("-20.69");
        assertThat(resposta.totalFinal()).isEqualByComparingTo("393.11");
        assertThat(resposta.parcelas()).isEqualTo(1);
        assertThat(resposta.valorParcela()).isEqualByComparingTo("393.11");
        assertThat(resposta.creditoProximaCompra()).isEqualByComparingTo("20.48");
        assertThat(resposta.brinde()).isFalse();
    }

    @Test
    void carrinhoVazioRetornaPedidoInvalido() {
        ResumoRequest request = new ResumoRequest(
                List.of(), "EXPRESSA", null, "PIX", null, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcular(request))
                .isInstanceOf(CheckoutException.class)
                .extracting("erro")
                .isEqualTo(ErroCheckout.PEDIDO_INVALIDO);
    }

    @Test
    void itemComQuantidadeZeroRetornaPedidoInvalido() {
        ItemPedidoRequest item = new ItemPedidoRequest("Boné", new BigDecimal("10.00"), 0, new BigDecimal("0.10"));
        ResumoRequest request = new ResumoRequest(
                List.of(item), "EXPRESSA", null, "PIX", null, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcular(request))
                .isInstanceOf(CheckoutException.class)
                .extracting("erro")
                .isEqualTo(ErroCheckout.PEDIDO_INVALIDO);
    }

    @Test
    void nivelClubeInvalido() {
        ResumoRequest request = new ResumoRequest(
                List.of(camiseta()), "EXPRESSA", null, "PIX", null, "DIAMANTE", "SUDESTE");

        assertThatThrownBy(() -> service.calcular(request))
                .isInstanceOf(CheckoutException.class)
                .extracting("erro")
                .isEqualTo(ErroCheckout.NIVEL_CLUBE_INVALIDO);
    }

    @Test
    void regiaoInvalida() {
        ResumoRequest request = new ResumoRequest(
                List.of(camiseta()), "EXPRESSA", null, "PIX", null, "BRONZE", "EXTERIOR");

        assertThatThrownBy(() -> service.calcular(request))
                .isInstanceOf(CheckoutException.class)
                .extracting("erro")
                .isEqualTo(ErroCheckout.REGIAO_INVALIDA);
    }

    @Test
    void modalidadeInvalida() {
        ResumoRequest request = new ResumoRequest(
                List.of(camiseta()), "TELETRANSPORTE", null, "PIX", null, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcular(request))
                .isInstanceOf(CheckoutException.class)
                .extracting("erro")
                .isEqualTo(ErroCheckout.MODALIDADE_INVALIDA);
    }

    @Test
    void motoboyAcimaDoPesoLimiteEhIndisponivel() {
        ItemPedidoRequest item = new ItemPedidoRequest("Caixa", new BigDecimal("50.00"), 1, new BigDecimal("6.00"));
        ResumoRequest request = new ResumoRequest(
                List.of(item), "MOTOBOY", null, "PIX", null, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcular(request))
                .isInstanceOf(CheckoutException.class)
                .extracting("erro")
                .isEqualTo(ErroCheckout.MODALIDADE_INDISPONIVEL);
    }

    @Test
    void cupomInvalido() {
        ResumoRequest request = new ResumoRequest(
                List.of(camiseta()), "EXPRESSA", "NAOEXISTE", "PIX", null, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcular(request))
                .isInstanceOf(CheckoutException.class)
                .extracting("erro")
                .isEqualTo(ErroCheckout.CUPOM_INVALIDO);
    }

    @Test
    void cupomNaoAplicavelAbaixoDoMinimo() {
        ResumoRequest request = new ResumoRequest(
                List.of(camiseta()), "EXPRESSA", "MENOS50", "PIX", null, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcular(request))
                .isInstanceOf(CheckoutException.class)
                .extracting("erro")
                .isEqualTo(ErroCheckout.CUPOM_NAO_APLICAVEL);
    }

    @Test
    void formaPagamentoInvalida() {
        ResumoRequest request = new ResumoRequest(
                List.of(camiseta()), "EXPRESSA", null, "CRIPTO", null, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcular(request))
                .isInstanceOf(CheckoutException.class)
                .extracting("erro")
                .isEqualTo(ErroCheckout.FORMA_PAGAMENTO_INVALIDA);
    }

    @Test
    void parcelamentoInvalidoParaPix() {
        ResumoRequest request = new ResumoRequest(
                List.of(camiseta()), "EXPRESSA", null, "PIX", 2, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcular(request))
                .isInstanceOf(CheckoutException.class)
                .extracting("erro")
                .isEqualTo(ErroCheckout.PARCELAMENTO_INVALIDO);
    }

    @Test
    void parcelamentoInvalidoParaCartaoAcimaDe12() {
        ResumoRequest request = new ResumoRequest(
                List.of(camiseta()), "EXPRESSA", null, "CARTAO", 13, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcular(request))
                .isInstanceOf(CheckoutException.class)
                .extracting("erro")
                .isEqualTo(ErroCheckout.PARCELAMENTO_INVALIDO);
    }

    @Test
    void boletoAcimaDoLimiteEhIndisponivel() {
        ItemPedidoRequest item = new ItemPedidoRequest("Notebook", new BigDecimal("1200.00"), 1, new BigDecimal("2.00"));
        ResumoRequest request = new ResumoRequest(
                List.of(item), "RETIRADA_LOJA", null, "BOLETO", null, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcular(request))
                .isInstanceOf(CheckoutException.class)
                .extracting("erro")
                .isEqualTo(ErroCheckout.FORMA_PAGAMENTO_INDISPONIVEL);
    }
}
