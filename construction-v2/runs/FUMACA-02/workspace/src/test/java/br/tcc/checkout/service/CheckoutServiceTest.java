package br.tcc.checkout.service;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import br.tcc.checkout.dto.ItemCarrinho;
import br.tcc.checkout.dto.RequisicaoResumo;
import br.tcc.checkout.dto.RespostaResumo;
import br.tcc.checkout.exception.ErroCheckout;

class CheckoutServiceTest {

    private CheckoutService service;

    @BeforeEach
    void setup() {
        service = new CheckoutService();
    }

    @Test
    void exemplo1_CamisetaTenisExpressaComCupomBemvindoPix() throws ErroCheckout {
        RequisicaoResumo requisicao = new RequisicaoResumo();
        requisicao.setItens(Arrays.asList(
            new ItemCarrinho("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
            new ItemCarrinho("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"))
        ));
        requisicao.setModalidadeEntrega("EXPRESSA");
        requisicao.setCupom("BEMVINDO10");
        requisicao.setFormaPagamento("PIX");
        requisicao.setParcelas(1);

        RespostaResumo resposta = service.calcularResumo(requisicao);

        assertEquals(new BigDecimal("409.70"), resposta.getSubtotalProdutos());
        assertEquals(new BigDecimal("40.97"), resposta.getDescontoCupom());
        assertEquals(new BigDecimal("33.10"), resposta.getFrete());
        assertEquals(2, resposta.getPrazoEntregaDias());
        assertEquals(new BigDecimal("-20.09"), resposta.getAjustePagamento());
        assertEquals(new BigDecimal("381.74"), resposta.getTotalFinal());
        assertEquals(1, resposta.getParcelas());
        assertEquals(new BigDecimal("381.74"), resposta.getValorParcela());
    }

    @Test
    void exemplo2_CamisetaTenisEconomicaSemCupomCartao6x() throws ErroCheckout {
        RequisicaoResumo requisicao = new RequisicaoResumo();
        requisicao.setItens(Arrays.asList(
            new ItemCarrinho("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
            new ItemCarrinho("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"))
        ));
        requisicao.setModalidadeEntrega("ECONOMICA");
        requisicao.setCupom(null);
        requisicao.setFormaPagamento("CARTAO");
        requisicao.setParcelas(6);

        RespostaResumo resposta = service.calcularResumo(requisicao);

        assertEquals(new BigDecimal("409.70"), resposta.getSubtotalProdutos());
        assertEquals(new BigDecimal("0.00"), resposta.getDescontoCupom());
        assertEquals(new BigDecimal("15.60"), resposta.getFrete());
        assertEquals(7, resposta.getPrazoEntregaDias());
        assertEquals(new BigDecimal("455.40"), resposta.getTotalFinal());
        assertEquals(6, resposta.getParcelas());
        assertEquals(new BigDecimal("75.90"), resposta.getValorParcela());
    }

    @Test
    void exemplo3_FonteMotoboyCupomMenos50Boleto() throws ErroCheckout {
        RequisicaoResumo requisicao = new RequisicaoResumo();
        requisicao.setItens(Arrays.asList(
            new ItemCarrinho("Fone", new BigDecimal("199.90"), 2, new BigDecimal("0.25"))
        ));
        requisicao.setModalidadeEntrega("MOTOBOY");
        requisicao.setCupom("MENOS50");
        requisicao.setFormaPagamento("BOLETO");
        requisicao.setParcelas(1);

        RespostaResumo resposta = service.calcularResumo(requisicao);

        assertEquals(new BigDecimal("399.80"), resposta.getSubtotalProdutos());
        assertEquals(new BigDecimal("50.00"), resposta.getDescontoCupom());
        assertEquals(new BigDecimal("18.00"), resposta.getFrete());
        assertEquals(0, resposta.getPrazoEntregaDias());
        assertEquals(new BigDecimal("3.49"), resposta.getAjustePagamento());
        assertEquals(new BigDecimal("371.29"), resposta.getTotalFinal());
        assertEquals(1, resposta.getParcelas());
        assertEquals(new BigDecimal("371.29"), resposta.getValorParcela());
    }

    @Test
    void exemplo4_MeiasCamisetaRetiraLojaLeve3Pague2Cartao3x() throws ErroCheckout {
        RequisicaoResumo requisicao = new RequisicaoResumo();
        requisicao.setItens(Arrays.asList(
            new ItemCarrinho("Meia", new BigDecimal("19.90"), 7, new BigDecimal("0.10")),
            new ItemCarrinho("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30"))
        ));
        requisicao.setModalidadeEntrega("RETIRADA_LOJA");
        requisicao.setCupom("LEVE3PAGUE2");
        requisicao.setFormaPagamento("CARTAO");
        requisicao.setParcelas(3);

        RespostaResumo resposta = service.calcularResumo(requisicao);

        assertEquals(new BigDecimal("299.10"), resposta.getSubtotalProdutos());
        assertEquals(new BigDecimal("39.80"), resposta.getDescontoCupom());
        assertEquals(new BigDecimal("0.00"), resposta.getFrete());
        assertEquals(1, resposta.getPrazoEntregaDias());
        assertEquals(new BigDecimal("0.00"), resposta.getAjustePagamento());
        assertEquals(new BigDecimal("259.30"), resposta.getTotalFinal());
        assertEquals(3, resposta.getParcelas());
        assertEquals(new BigDecimal("86.43"), resposta.getValorParcela());
    }

    @Test
    void pedidoVazio() {
        RequisicaoResumo requisicao = new RequisicaoResumo();
        requisicao.setItens(Arrays.asList());
        requisicao.setModalidadeEntrega("EXPRESSA");
        requisicao.setFormaPagamento("PIX");

        ErroCheckout erro = assertThrows(ErroCheckout.class, () -> service.calcularResumo(requisicao));
        assertEquals("PEDIDO_INVALIDO", erro.getCodigo());
    }

    @Test
    void itemComPrecoZero() {
        RequisicaoResumo requisicao = new RequisicaoResumo();
        requisicao.setItens(Arrays.asList(
            new ItemCarrinho("Produto", new BigDecimal("0.00"), 1, new BigDecimal("0.5"))
        ));
        requisicao.setModalidadeEntrega("EXPRESSA");
        requisicao.setFormaPagamento("PIX");

        ErroCheckout erro = assertThrows(ErroCheckout.class, () -> service.calcularResumo(requisicao));
        assertEquals("PEDIDO_INVALIDO", erro.getCodigo());
    }

    @Test
    void itemComQuantidadeZero() {
        RequisicaoResumo requisicao = new RequisicaoResumo();
        requisicao.setItens(Arrays.asList(
            new ItemCarrinho("Produto", new BigDecimal("10.00"), 0, new BigDecimal("0.5"))
        ));
        requisicao.setModalidadeEntrega("EXPRESSA");
        requisicao.setFormaPagamento("PIX");

        ErroCheckout erro = assertThrows(ErroCheckout.class, () -> service.calcularResumo(requisicao));
        assertEquals("PEDIDO_INVALIDO", erro.getCodigo());
    }

    @Test
    void itemComPesoZero() {
        RequisicaoResumo requisicao = new RequisicaoResumo();
        requisicao.setItens(Arrays.asList(
            new ItemCarrinho("Produto", new BigDecimal("10.00"), 1, new BigDecimal("0.00"))
        ));
        requisicao.setModalidadeEntrega("EXPRESSA");
        requisicao.setFormaPagamento("PIX");

        ErroCheckout erro = assertThrows(ErroCheckout.class, () -> service.calcularResumo(requisicao));
        assertEquals("PEDIDO_INVALIDO", erro.getCodigo());
    }

    @Test
    void modalidadeInvalida() {
        RequisicaoResumo requisicao = new RequisicaoResumo();
        requisicao.setItens(Arrays.asList(
            new ItemCarrinho("Produto", new BigDecimal("10.00"), 1, new BigDecimal("0.5"))
        ));
        requisicao.setModalidadeEntrega("INVALIDA");
        requisicao.setFormaPagamento("PIX");

        ErroCheckout erro = assertThrows(ErroCheckout.class, () -> service.calcularResumo(requisicao));
        assertEquals("MODALIDADE_INVALIDA", erro.getCodigo());
    }

    @Test
    void modalidadeNaoInformada() {
        RequisicaoResumo requisicao = new RequisicaoResumo();
        requisicao.setItens(Arrays.asList(
            new ItemCarrinho("Produto", new BigDecimal("10.00"), 1, new BigDecimal("0.5"))
        ));
        requisicao.setModalidadeEntrega(null);
        requisicao.setFormaPagamento("PIX");

        ErroCheckout erro = assertThrows(ErroCheckout.class, () -> service.calcularResumo(requisicao));
        assertEquals("MODALIDADE_INVALIDA", erro.getCodigo());
    }

    @Test
    void motoboyComPesoAcimaDeLimite() {
        RequisicaoResumo requisicao = new RequisicaoResumo();
        requisicao.setItens(Arrays.asList(
            new ItemCarrinho("Produto", new BigDecimal("10.00"), 1, new BigDecimal("5.50"))
        ));
        requisicao.setModalidadeEntrega("MOTOBOY");
        requisicao.setFormaPagamento("PIX");

        ErroCheckout erro = assertThrows(ErroCheckout.class, () -> service.calcularResumo(requisicao));
        assertEquals("MODALIDADE_INDISPONIVEL", erro.getCodigo());
    }

    @Test
    void cupomInvalido() {
        RequisicaoResumo requisicao = new RequisicaoResumo();
        requisicao.setItens(Arrays.asList(
            new ItemCarrinho("Produto", new BigDecimal("10.00"), 1, new BigDecimal("0.5"))
        ));
        requisicao.setModalidadeEntrega("EXPRESSA");
        requisicao.setCupom("INVALIDO");
        requisicao.setFormaPagamento("PIX");

        ErroCheckout erro = assertThrows(ErroCheckout.class, () -> service.calcularResumo(requisicao));
        assertEquals("CUPOM_INVALIDO", erro.getCodigo());
    }

    @Test
    void cupomMenos50NaoAplicavel() {
        RequisicaoResumo requisicao = new RequisicaoResumo();
        requisicao.setItens(Arrays.asList(
            new ItemCarrinho("Produto", new BigDecimal("100.00"), 1, new BigDecimal("0.5"))
        ));
        requisicao.setModalidadeEntrega("EXPRESSA");
        requisicao.setCupom("MENOS50");
        requisicao.setFormaPagamento("PIX");

        ErroCheckout erro = assertThrows(ErroCheckout.class, () -> service.calcularResumo(requisicao));
        assertEquals("CUPOM_NAO_APLICAVEL", erro.getCodigo());
    }

    @Test
    void formaPagamentoInvalida() {
        RequisicaoResumo requisicao = new RequisicaoResumo();
        requisicao.setItens(Arrays.asList(
            new ItemCarrinho("Produto", new BigDecimal("10.00"), 1, new BigDecimal("0.5"))
        ));
        requisicao.setModalidadeEntrega("EXPRESSA");
        requisicao.setFormaPagamento("INVALIDA");

        ErroCheckout erro = assertThrows(ErroCheckout.class, () -> service.calcularResumo(requisicao));
        assertEquals("FORMA_PAGAMENTO_INVALIDA", erro.getCodigo());
    }

    @Test
    void formaPagamentoNaoInformada() {
        RequisicaoResumo requisicao = new RequisicaoResumo();
        requisicao.setItens(Arrays.asList(
            new ItemCarrinho("Produto", new BigDecimal("10.00"), 1, new BigDecimal("0.5"))
        ));
        requisicao.setModalidadeEntrega("EXPRESSA");
        requisicao.setFormaPagamento(null);

        ErroCheckout erro = assertThrows(ErroCheckout.class, () -> service.calcularResumo(requisicao));
        assertEquals("FORMA_PAGAMENTO_INVALIDA", erro.getCodigo());
    }

    @Test
    void parcelamentoInvalidoPixAcimaDeUma() {
        RequisicaoResumo requisicao = new RequisicaoResumo();
        requisicao.setItens(Arrays.asList(
            new ItemCarrinho("Produto", new BigDecimal("10.00"), 1, new BigDecimal("0.5"))
        ));
        requisicao.setModalidadeEntrega("EXPRESSA");
        requisicao.setFormaPagamento("PIX");
        requisicao.setParcelas(2);

        ErroCheckout erro = assertThrows(ErroCheckout.class, () -> service.calcularResumo(requisicao));
        assertEquals("PARCELAMENTO_INVALIDO", erro.getCodigo());
    }

    @Test
    void parcelamentoInvalidoBoletoAcimaDeUma() {
        RequisicaoResumo requisicao = new RequisicaoResumo();
        requisicao.setItens(Arrays.asList(
            new ItemCarrinho("Produto", new BigDecimal("10.00"), 1, new BigDecimal("0.5"))
        ));
        requisicao.setModalidadeEntrega("EXPRESSA");
        requisicao.setFormaPagamento("BOLETO");
        requisicao.setParcelas(2);

        ErroCheckout erro = assertThrows(ErroCheckout.class, () -> service.calcularResumo(requisicao));
        assertEquals("PARCELAMENTO_INVALIDO", erro.getCodigo());
    }

    @Test
    void parcelamentoInvalidoCartaoAcimaDeDoze() {
        RequisicaoResumo requisicao = new RequisicaoResumo();
        requisicao.setItens(Arrays.asList(
            new ItemCarrinho("Produto", new BigDecimal("10.00"), 1, new BigDecimal("0.5"))
        ));
        requisicao.setModalidadeEntrega("EXPRESSA");
        requisicao.setFormaPagamento("CARTAO");
        requisicao.setParcelas(13);

        ErroCheckout erro = assertThrows(ErroCheckout.class, () -> service.calcularResumo(requisicao));
        assertEquals("PARCELAMENTO_INVALIDO", erro.getCodigo());
    }

    @Test
    void formaPagamentoIndisponipelBoletoAcimaDeMilleira() {
        RequisicaoResumo requisicao = new RequisicaoResumo();
        requisicao.setItens(Arrays.asList(
            new ItemCarrinho("Produto", new BigDecimal("1000.01"), 1, new BigDecimal("0.5"))
        ));
        requisicao.setModalidadeEntrega("RETIRADA_LOJA");
        requisicao.setFormaPagamento("BOLETO");

        ErroCheckout erro = assertThrows(ErroCheckout.class, () -> service.calcularResumo(requisicao));
        assertEquals("FORMA_PAGAMENTO_INDISPONIVEL", erro.getCodigo());
    }

    @Test
    void parcelasNulaDefineComUma() throws ErroCheckout {
        RequisicaoResumo requisicao = new RequisicaoResumo();
        requisicao.setItens(Arrays.asList(
            new ItemCarrinho("Produto", new BigDecimal("100.00"), 1, new BigDecimal("0.5"))
        ));
        requisicao.setModalidadeEntrega("RETIRADA_LOJA");
        requisicao.setFormaPagamento("PIX");
        requisicao.setParcelas(null);

        RespostaResumo resposta = service.calcularResumo(requisicao);
        assertEquals(1, resposta.getParcelas());
    }
}
