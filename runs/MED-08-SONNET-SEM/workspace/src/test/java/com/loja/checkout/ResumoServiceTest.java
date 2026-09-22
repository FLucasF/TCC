package com.loja.checkout;

import com.loja.checkout.cupom.CupomBemVindo10;
import com.loja.checkout.cupom.CupomFreteGratis;
import com.loja.checkout.cupom.CupomLeve3Pague2;
import com.loja.checkout.cupom.CupomMenos50;
import com.loja.checkout.cupom.CupomService;
import com.loja.checkout.dto.ItemRequest;
import com.loja.checkout.dto.ResumoRequest;
import com.loja.checkout.dto.ResumoResponse;
import com.loja.checkout.entrega.EntregaEconomica;
import com.loja.checkout.entrega.EntregaExpressa;
import com.loja.checkout.entrega.EntregaMotoboy;
import com.loja.checkout.entrega.EntregaRetiradaLoja;
import com.loja.checkout.entrega.EntregaService;
import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.exception.CodigoErro;
import com.loja.checkout.pagamento.PagamentoBoleto;
import com.loja.checkout.pagamento.PagamentoCartao;
import com.loja.checkout.pagamento.PagamentoPix;
import com.loja.checkout.pagamento.PagamentoService;
import com.loja.checkout.service.ResumoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResumoServiceTest {

    private ResumoService resumoService;

    @BeforeEach
    void montarService() {
        EntregaService entregaService = new EntregaService(List.of(
                new EntregaEconomica(), new EntregaExpressa(), new EntregaRetiradaLoja(), new EntregaMotoboy()));
        CupomService cupomService = new CupomService(List.of(
                new CupomBemVindo10(), new CupomMenos50(), new CupomFreteGratis(), new CupomLeve3Pague2()));
        PagamentoService pagamentoService = new PagamentoService(List.of(
                new PagamentoPix(), new PagamentoCartao(), new PagamentoBoleto()));
        resumoService = new ResumoService(entregaService, cupomService, pagamentoService);
    }

    private ItemRequest item(String nome, String preco, int quantidade, String peso) {
        return new ItemRequest(nome, new BigDecimal(preco), quantidade, new BigDecimal(peso));
    }

    @Test
    void exemplo1_expressa_bemvindo10_pix() {
        ResumoRequest request = new ResumoRequest(
                List.of(item("Camiseta", "79.90", 2, "0.30"), item("Tênis", "249.90", 1, "1.20")),
                "EXPRESSA", "BEMVINDO10", "PIX", 1);

        ResumoResponse resposta = resumoService.calcular(request);

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
    void exemplo2_economica_semCupom_cartao6x() {
        ResumoRequest request = new ResumoRequest(
                List.of(item("Camiseta", "79.90", 2, "0.30"), item("Tênis", "249.90", 1, "1.20")),
                "ECONOMICA", null, "CARTAO", 6);

        ResumoResponse resposta = resumoService.calcular(request);

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
    void exemplo3_motoboy_menos50_boleto() {
        ResumoRequest request = new ResumoRequest(
                List.of(item("Fone", "199.90", 2, "0.25")),
                "MOTOBOY", "MENOS50", "BOLETO", null);

        ResumoResponse resposta = resumoService.calcular(request);

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
    void exemplo4_retiradaLoja_leve3pague2_cartao3x() {
        ResumoRequest request = new ResumoRequest(
                List.of(item("Meia", "19.90", 7, "0.10"), item("Camiseta", "79.90", 2, "0.30")),
                "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3);

        ResumoResponse resposta = resumoService.calcular(request);

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
    void carrinhoVazio_retornaPedidoInvalido() {
        ResumoRequest request = new ResumoRequest(List.of(), "ECONOMICA", null, "PIX", 1);

        assertThatThrownBy(() -> resumoService.calcular(request))
                .isInstanceOf(CheckoutException.class)
                .extracting(erro -> ((CheckoutException) erro).getCodigo())
                .isEqualTo(CodigoErro.PEDIDO_INVALIDO);
    }

    @Test
    void itemComQuantidadeZero_retornaPedidoInvalido() {
        ResumoRequest request = new ResumoRequest(
                List.of(item("Camiseta", "79.90", 0, "0.30")), "ECONOMICA", null, "PIX", 1);

        assertThatThrownBy(() -> resumoService.calcular(request))
                .isInstanceOf(CheckoutException.class)
                .extracting(erro -> ((CheckoutException) erro).getCodigo())
                .isEqualTo(CodigoErro.PEDIDO_INVALIDO);
    }

    @Test
    void modalidadeInexistente_retornaModalidadeInvalida() {
        ResumoRequest request = new ResumoRequest(
                List.of(item("Camiseta", "79.90", 1, "0.30")), "TELEPORTE", null, "PIX", 1);

        assertThatThrownBy(() -> resumoService.calcular(request))
                .isInstanceOf(CheckoutException.class)
                .extracting(erro -> ((CheckoutException) erro).getCodigo())
                .isEqualTo(CodigoErro.MODALIDADE_INVALIDA);
    }

    @Test
    void motoboyAcimaDoPeso_retornaModalidadeIndisponivel() {
        ResumoRequest request = new ResumoRequest(
                List.of(item("Fogão", "500.00", 1, "6.00")), "MOTOBOY", null, "PIX", 1);

        assertThatThrownBy(() -> resumoService.calcular(request))
                .isInstanceOf(CheckoutException.class)
                .extracting(erro -> ((CheckoutException) erro).getCodigo())
                .isEqualTo(CodigoErro.MODALIDADE_INDISPONIVEL);
    }

    @Test
    void cupomInexistente_retornaCupomInvalido() {
        ResumoRequest request = new ResumoRequest(
                List.of(item("Camiseta", "79.90", 1, "0.30")), "ECONOMICA", "NAOEXISTE", "PIX", 1);

        assertThatThrownBy(() -> resumoService.calcular(request))
                .isInstanceOf(CheckoutException.class)
                .extracting(erro -> ((CheckoutException) erro).getCodigo())
                .isEqualTo(CodigoErro.CUPOM_INVALIDO);
    }

    @Test
    void menos50AbaixoDoMinimo_retornaCupomNaoAplicavel() {
        ResumoRequest request = new ResumoRequest(
                List.of(item("Camiseta", "79.90", 1, "0.30")), "ECONOMICA", "MENOS50", "PIX", 1);

        assertThatThrownBy(() -> resumoService.calcular(request))
                .isInstanceOf(CheckoutException.class)
                .extracting(erro -> ((CheckoutException) erro).getCodigo())
                .isEqualTo(CodigoErro.CUPOM_NAO_APLICAVEL);
    }

    @Test
    void formaPagamentoInexistente_retornaFormaPagamentoInvalida() {
        ResumoRequest request = new ResumoRequest(
                List.of(item("Camiseta", "79.90", 1, "0.30")), "ECONOMICA", null, "DINHEIRO", 1);

        assertThatThrownBy(() -> resumoService.calcular(request))
                .isInstanceOf(CheckoutException.class)
                .extracting(erro -> ((CheckoutException) erro).getCodigo())
                .isEqualTo(CodigoErro.FORMA_PAGAMENTO_INVALIDA);
    }

    @Test
    void pixComParcelamento_retornaParcelamentoInvalido() {
        ResumoRequest request = new ResumoRequest(
                List.of(item("Camiseta", "79.90", 1, "0.30")), "ECONOMICA", null, "PIX", 2);

        assertThatThrownBy(() -> resumoService.calcular(request))
                .isInstanceOf(CheckoutException.class)
                .extracting(erro -> ((CheckoutException) erro).getCodigo())
                .isEqualTo(CodigoErro.PARCELAMENTO_INVALIDO);
    }

    @Test
    void cartaoAcimaDe12Parcelas_retornaParcelamentoInvalido() {
        ResumoRequest request = new ResumoRequest(
                List.of(item("Camiseta", "79.90", 1, "0.30")), "ECONOMICA", null, "CARTAO", 13);

        assertThatThrownBy(() -> resumoService.calcular(request))
                .isInstanceOf(CheckoutException.class)
                .extracting(erro -> ((CheckoutException) erro).getCodigo())
                .isEqualTo(CodigoErro.PARCELAMENTO_INVALIDO);
    }

    @Test
    void boletoAcimaDoLimite_retornaFormaPagamentoIndisponivel() {
        ResumoRequest request = new ResumoRequest(
                List.of(item("Notebook", "6000.00", 1, "2.00")), "RETIRADA_LOJA", null, "BOLETO", 1);

        assertThatThrownBy(() -> resumoService.calcular(request))
                .isInstanceOf(CheckoutException.class)
                .extracting(erro -> ((CheckoutException) erro).getCodigo())
                .isEqualTo(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
    }

    @Test
    void freteGratis_descontoIgualAoFrete() {
        ResumoRequest request = new ResumoRequest(
                List.of(item("Camiseta", "79.90", 1, "0.30")), "EXPRESSA", "FRETEGRATIS", "PIX", 1);

        ResumoResponse resposta = resumoService.calcular(request);

        assertThat(resposta.descontoCupom()).isEqualByComparingTo(resposta.frete());
    }
}
