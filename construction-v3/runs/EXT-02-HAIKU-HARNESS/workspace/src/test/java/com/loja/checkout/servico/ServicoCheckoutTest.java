package com.loja.checkout.servico;

import com.loja.checkout.dto.ItemPedidoDTO;
import com.loja.checkout.dto.RequisicaoResumoDTO;
import com.loja.checkout.dto.RespostaResumoDTO;
import com.loja.checkout.excecao.ErroCheckout;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class ServicoCheckoutTest {

    private ServicoCheckout servico;

    @BeforeEach
    void setUp() {
        servico = new ServicoCheckout();
    }

    @Test
    void exemplo1_CamisetaTenisExpressaBemVindo10Pix() {
        RequisicaoResumoDTO requisicao = new RequisicaoResumoDTO();
        requisicao.setItens(Arrays.asList(
            new ItemPedidoDTO("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
            new ItemPedidoDTO("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"))
        ));
        requisicao.setModalidadeEntrega("EXPRESSA");
        requisicao.setCupom("BEMVINDO10");
        requisicao.setFormaPagamento("PIX");
        requisicao.setNivelClube("BRONZE");
        requisicao.setRegiao("SUDESTE");

        RespostaResumoDTO resposta = servico.calcularResumo(requisicao);

        assertEquals(new BigDecimal("409.70"), resposta.getSubtotalProdutos());
        assertEquals(new BigDecimal("40.97"), resposta.getDescontoCupom());
        assertEquals(new BigDecimal("33.10"), resposta.getFrete());
        assertEquals(2, resposta.getPrazoEntregaDias());
        assertEquals(new BigDecimal("44.25"), resposta.getImposto());
        assertEquals(new BigDecimal("423.78"), resposta.getTotalFinal());
        assertEquals(1, resposta.getParcelas());
    }

    @Test
    void exemplo2_CamisetaTenisEconomicaSemCupomCartao6x() {
        RequisicaoResumoDTO requisicao = new RequisicaoResumoDTO();
        requisicao.setItens(Arrays.asList(
            new ItemPedidoDTO("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
            new ItemPedidoDTO("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"))
        ));
        requisicao.setModalidadeEntrega("ECONOMICA");
        requisicao.setFormaPagamento("CARTAO");
        requisicao.setParcelas(6);
        requisicao.setNivelClube("BRONZE");
        requisicao.setRegiao("SUDESTE");

        RespostaResumoDTO resposta = servico.calcularResumo(requisicao);

        assertEquals(new BigDecimal("409.70"), resposta.getSubtotalProdutos());
        assertEquals(BigDecimal.ZERO, resposta.getDescontoCupom());
        assertEquals(new BigDecimal("15.60"), resposta.getFrete());
        assertEquals(7, resposta.getPrazoEntregaDias());
        assertEquals(new BigDecimal("49.16"), resposta.getImposto());
        assertEquals(6, resposta.getParcelas());
    }

    @Test
    void exemplo3_FoneMotoboymenos50Boleto() {
        RequisicaoResumoDTO requisicao = new RequisicaoResumoDTO();
        requisicao.setItens(Collections.singletonList(
            new ItemPedidoDTO("Fone", new BigDecimal("199.90"), 2, new BigDecimal("0.25"))
        ));
        requisicao.setModalidadeEntrega("MOTOBOY");
        requisicao.setCupom("MENOS50");
        requisicao.setFormaPagamento("BOLETO");
        requisicao.setNivelClube("BRONZE");
        requisicao.setRegiao("SUDESTE");

        RespostaResumoDTO resposta = servico.calcularResumo(requisicao);

        assertEquals(new BigDecimal("399.80"), resposta.getSubtotalProdutos());
        assertEquals(new BigDecimal("50.00"), resposta.getDescontoCupom());
        assertEquals(new BigDecimal("18.00"), resposta.getFrete());
        assertEquals(0, resposta.getPrazoEntregaDias());
        assertEquals(new BigDecimal("41.98"), resposta.getImposto());
        assertEquals(new BigDecimal("413.27"), resposta.getTotalFinal());
        assertEquals(1, resposta.getParcelas());
    }

    @Test
    void exemplo4_MeiasCamisetasRetiraLojaaLeve3Pague2Cartao3x() {
        RequisicaoResumoDTO requisicao = new RequisicaoResumoDTO();
        requisicao.setItens(Arrays.asList(
            new ItemPedidoDTO("Meia", new BigDecimal("19.90"), 7, new BigDecimal("0.10")),
            new ItemPedidoDTO("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30"))
        ));
        requisicao.setModalidadeEntrega("RETIRADA_LOJA");
        requisicao.setCupom("LEVE3PAGUE2");
        requisicao.setFormaPagamento("CARTAO");
        requisicao.setParcelas(3);
        requisicao.setNivelClube("BRONZE");
        requisicao.setRegiao("SUDESTE");

        RespostaResumoDTO resposta = servico.calcularResumo(requisicao);

        assertEquals(new BigDecimal("299.10"), resposta.getSubtotalProdutos());
        assertEquals(new BigDecimal("39.80"), resposta.getDescontoCupom());
        assertEquals(new BigDecimal("0.00"), resposta.getFrete());
        assertEquals(1, resposta.getPrazoEntregaDias());
        assertEquals(new BigDecimal("31.12"), resposta.getImposto());
        assertEquals(3, resposta.getParcelas());
    }

    @Test
    void exemplo5_CamisetaTenisExpressaSemCupomPixOuroSudeste() {
        RequisicaoResumoDTO requisicao = new RequisicaoResumoDTO();
        requisicao.setItens(Arrays.asList(
            new ItemPedidoDTO("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
            new ItemPedidoDTO("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"))
        ));
        requisicao.setModalidadeEntrega("EXPRESSA");
        requisicao.setFormaPagamento("PIX");
        requisicao.setNivelClube("OURO");
        requisicao.setRegiao("SUDESTE");

        RespostaResumoDTO resposta = servico.calcularResumo(requisicao);

        assertEquals(new BigDecimal("409.70"), resposta.getSubtotalProdutos());
        assertEquals(BigDecimal.ZERO, resposta.getDescontoCupom());
        assertEquals(BigDecimal.ZERO, resposta.getFrete());
        assertEquals(2, resposta.getPrazoEntregaDias());
        assertEquals(new BigDecimal("49.16"), resposta.getImposto());
        assertEquals(new BigDecimal("435.92"), resposta.getTotalFinal());
        assertEquals(1, resposta.getParcelas());
        assertEquals(new BigDecimal("435.92"), resposta.getValorParcela());
        assertEquals(new BigDecimal("20.48"), resposta.getCreditoProximaCompra());
        assertEquals(false, resposta.getBrinde());
    }

    @Test
    void pedidoInvalido_CarrinhoVazio() {
        RequisicaoResumoDTO requisicao = new RequisicaoResumoDTO();
        requisicao.setItens(Collections.emptyList());
        requisicao.setModalidadeEntrega("EXPRESSA");
        requisicao.setFormaPagamento("PIX");
        requisicao.setNivelClube("BRONZE");
        requisicao.setRegiao("SUDESTE");

        ErroCheckout erro = assertThrows(ErroCheckout.class, () -> servico.calcularResumo(requisicao));
        assertEquals("PEDIDO_INVALIDO", erro.getCodigo());
    }

    @Test
    void nivelClubeInvalido() {
        RequisicaoResumoDTO requisicao = new RequisicaoResumoDTO();
        requisicao.setItens(Arrays.asList(
            new ItemPedidoDTO("Camiseta", new BigDecimal("79.90"), 1, new BigDecimal("0.30"))
        ));
        requisicao.setModalidadeEntrega("EXPRESSA");
        requisicao.setFormaPagamento("PIX");
        requisicao.setNivelClube("INVALIDO");
        requisicao.setRegiao("SUDESTE");

        ErroCheckout erro = assertThrows(ErroCheckout.class, () -> servico.calcularResumo(requisicao));
        assertEquals("NIVEL_CLUBE_INVALIDO", erro.getCodigo());
    }

    @Test
    void regiaoInvalida() {
        RequisicaoResumoDTO requisicao = new RequisicaoResumoDTO();
        requisicao.setItens(Arrays.asList(
            new ItemPedidoDTO("Camiseta", new BigDecimal("79.90"), 1, new BigDecimal("0.30"))
        ));
        requisicao.setModalidadeEntrega("EXPRESSA");
        requisicao.setFormaPagamento("PIX");
        requisicao.setNivelClube("BRONZE");
        requisicao.setRegiao("INVALIDA");

        ErroCheckout erro = assertThrows(ErroCheckout.class, () -> servico.calcularResumo(requisicao));
        assertEquals("REGIAO_INVALIDA", erro.getCodigo());
    }

    @Test
    void modalidadeInvalida() {
        RequisicaoResumoDTO requisicao = new RequisicaoResumoDTO();
        requisicao.setItens(Arrays.asList(
            new ItemPedidoDTO("Camiseta", new BigDecimal("79.90"), 1, new BigDecimal("0.30"))
        ));
        requisicao.setModalidadeEntrega("INVALIDA");
        requisicao.setFormaPagamento("PIX");
        requisicao.setNivelClube("BRONZE");
        requisicao.setRegiao("SUDESTE");

        ErroCheckout erro = assertThrows(ErroCheckout.class, () -> servico.calcularResumo(requisicao));
        assertEquals("MODALIDADE_INVALIDA", erro.getCodigo());
    }

    @Test
    void modalidadeIndisponivel_MotoboySobrepeso() {
        RequisicaoResumoDTO requisicao = new RequisicaoResumoDTO();
        requisicao.setItens(Arrays.asList(
            new ItemPedidoDTO("Mochila", new BigDecimal("200.00"), 3, new BigDecimal("2.00"))
        ));
        requisicao.setModalidadeEntrega("MOTOBOY");
        requisicao.setFormaPagamento("PIX");
        requisicao.setNivelClube("BRONZE");
        requisicao.setRegiao("SUDESTE");

        ErroCheckout erro = assertThrows(ErroCheckout.class, () -> servico.calcularResumo(requisicao));
        assertEquals("MODALIDADE_INDISPONIVEL", erro.getCodigo());
    }

    @Test
    void cupomInvalido() {
        RequisicaoResumoDTO requisicao = new RequisicaoResumoDTO();
        requisicao.setItens(Arrays.asList(
            new ItemPedidoDTO("Camiseta", new BigDecimal("79.90"), 1, new BigDecimal("0.30"))
        ));
        requisicao.setModalidadeEntrega("EXPRESSA");
        requisicao.setCupom("CUPOMINVALIDO");
        requisicao.setFormaPagamento("PIX");
        requisicao.setNivelClube("BRONZE");
        requisicao.setRegiao("SUDESTE");

        ErroCheckout erro = assertThrows(ErroCheckout.class, () -> servico.calcularResumo(requisicao));
        assertEquals("CUPOM_INVALIDO", erro.getCodigo());
    }

    @Test
    void cupomNaoAplicavel_Menos50SemMinimo() {
        RequisicaoResumoDTO requisicao = new RequisicaoResumoDTO();
        requisicao.setItens(Arrays.asList(
            new ItemPedidoDTO("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30"))
        ));
        requisicao.setModalidadeEntrega("EXPRESSA");
        requisicao.setCupom("MENOS50");
        requisicao.setFormaPagamento("PIX");
        requisicao.setNivelClube("BRONZE");
        requisicao.setRegiao("SUDESTE");

        ErroCheckout erro = assertThrows(ErroCheckout.class, () -> servico.calcularResumo(requisicao));
        assertEquals("CUPOM_NAO_APLICAVEL", erro.getCodigo());
    }

    @Test
    void formaPagamentoInvalida() {
        RequisicaoResumoDTO requisicao = new RequisicaoResumoDTO();
        requisicao.setItens(Arrays.asList(
            new ItemPedidoDTO("Camiseta", new BigDecimal("79.90"), 1, new BigDecimal("0.30"))
        ));
        requisicao.setModalidadeEntrega("EXPRESSA");
        requisicao.setFormaPagamento("INVALIDA");
        requisicao.setNivelClube("BRONZE");
        requisicao.setRegiao("SUDESTE");

        ErroCheckout erro = assertThrows(ErroCheckout.class, () -> servico.calcularResumo(requisicao));
        assertEquals("FORMA_PAGAMENTO_INVALIDA", erro.getCodigo());
    }

    @Test
    void parcelamentoInvalido_PixComParcelas() {
        RequisicaoResumoDTO requisicao = new RequisicaoResumoDTO();
        requisicao.setItens(Arrays.asList(
            new ItemPedidoDTO("Camiseta", new BigDecimal("79.90"), 1, new BigDecimal("0.30"))
        ));
        requisicao.setModalidadeEntrega("EXPRESSA");
        requisicao.setFormaPagamento("PIX");
        requisicao.setParcelas(2);
        requisicao.setNivelClube("BRONZE");
        requisicao.setRegiao("SUDESTE");

        ErroCheckout erro = assertThrows(ErroCheckout.class, () -> servico.calcularResumo(requisicao));
        assertEquals("PARCELAMENTO_INVALIDO", erro.getCodigo());
    }

    @Test
    void formaPagamentoIndisponivel_BoletoAcimaDoLimite() {
        RequisicaoResumoDTO requisicao = new RequisicaoResumoDTO();
        requisicao.setItens(Arrays.asList(
            new ItemPedidoDTO("Produto", new BigDecimal("500.00"), 3, new BigDecimal("1.00"))
        ));
        requisicao.setModalidadeEntrega("EXPRESSA");
        requisicao.setFormaPagamento("BOLETO");
        requisicao.setNivelClube("BRONZE");
        requisicao.setRegiao("SUDESTE");

        ErroCheckout erro = assertThrows(ErroCheckout.class, () -> servico.calcularResumo(requisicao));
        assertEquals("FORMA_PAGAMENTO_INDISPONIVEL", erro.getCodigo());
    }

    @Test
    void cupomFreteGratis() {
        RequisicaoResumoDTO requisicao = new RequisicaoResumoDTO();
        requisicao.setItens(Arrays.asList(
            new ItemPedidoDTO("Camiseta", new BigDecimal("100.00"), 1, new BigDecimal("0.50"))
        ));
        requisicao.setModalidadeEntrega("EXPRESSA");
        requisicao.setCupom("FRETEGRATIS");
        requisicao.setFormaPagamento("PIX");
        requisicao.setNivelClube("BRONZE");
        requisicao.setRegiao("SUDESTE");

        RespostaResumoDTO resposta = servico.calcularResumo(requisicao);

        assertEquals(new BigDecimal("100.00"), resposta.getSubtotalProdutos());
        assertEquals(new BigDecimal("27.25"), resposta.getFrete());
        assertEquals(new BigDecimal("27.25"), resposta.getDescontoCupom());
    }

    @Test
    void ouroComBrinde() {
        RequisicaoResumoDTO requisicao = new RequisicaoResumoDTO();
        requisicao.setItens(Arrays.asList(
            new ItemPedidoDTO("Produto", new BigDecimal("300.00"), 2, new BigDecimal("1.00"))
        ));
        requisicao.setModalidadeEntrega("RETIRADA_LOJA");
        requisicao.setFormaPagamento("PIX");
        requisicao.setNivelClube("OURO");
        requisicao.setRegiao("SUDESTE");

        RespostaResumoDTO resposta = servico.calcularResumo(requisicao);

        assertEquals(true, resposta.getBrinde());
    }

    @Test
    void ouroSemBrinde_AbaixoDoLimite() {
        RequisicaoResumoDTO requisicao = new RequisicaoResumoDTO();
        requisicao.setItens(Arrays.asList(
            new ItemPedidoDTO("Produto", new BigDecimal("200.00"), 2, new BigDecimal("1.00"))
        ));
        requisicao.setModalidadeEntrega("RETIRADA_LOJA");
        requisicao.setFormaPagamento("PIX");
        requisicao.setNivelClube("OURO");
        requisicao.setRegiao("SUDESTE");

        RespostaResumoDTO resposta = servico.calcularResumo(requisicao);

        assertEquals(false, resposta.getBrinde());
    }

    @Test
    void creditoPrata() {
        RequisicaoResumoDTO requisicao = new RequisicaoResumoDTO();
        requisicao.setItens(Arrays.asList(
            new ItemPedidoDTO("Produto", new BigDecimal("100.00"), 1, new BigDecimal("1.00"))
        ));
        requisicao.setModalidadeEntrega("RETIRADA_LOJA");
        requisicao.setFormaPagamento("PIX");
        requisicao.setNivelClube("PRATA");
        requisicao.setRegiao("SUDESTE");

        RespostaResumoDTO resposta = servico.calcularResumo(requisicao);

        assertEquals(new BigDecimal("2.00"), resposta.getCreditoProximaCompra());
    }

    @Test
    void descontoPixAplicado() {
        RequisicaoResumoDTO requisicao = new RequisicaoResumoDTO();
        requisicao.setItens(Arrays.asList(
            new ItemPedidoDTO("Produto", new BigDecimal("100.00"), 1, new BigDecimal("1.00"))
        ));
        requisicao.setModalidadeEntrega("RETIRADA_LOJA");
        requisicao.setFormaPagamento("PIX");
        requisicao.setNivelClube("BRONZE");
        requisicao.setRegiao("SUDESTE");

        RespostaResumoDTO resposta = servico.calcularResumo(requisicao);

        assertEquals(new BigDecimal("-5.60"), resposta.getAjustePagamento());
        assertEquals(new BigDecimal("106.40"), resposta.getTotalFinal());
    }

    @Test
    void tarifaBoletoAdicionada() {
        RequisicaoResumoDTO requisicao = new RequisicaoResumoDTO();
        requisicao.setItens(Arrays.asList(
            new ItemPedidoDTO("Produto", new BigDecimal("100.00"), 1, new BigDecimal("1.00"))
        ));
        requisicao.setModalidadeEntrega("RETIRADA_LOJA");
        requisicao.setFormaPagamento("BOLETO");
        requisicao.setNivelClube("BRONZE");
        requisicao.setRegiao("SUDESTE");

        RespostaResumoDTO resposta = servico.calcularResumo(requisicao);

        assertEquals(new BigDecimal("3.49"), resposta.getAjustePagamento());
    }
}
