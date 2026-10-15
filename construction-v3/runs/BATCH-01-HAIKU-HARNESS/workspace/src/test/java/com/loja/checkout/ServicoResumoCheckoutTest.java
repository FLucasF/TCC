package com.loja.checkout;

import com.loja.checkout.domain.*;
import com.loja.checkout.exception.ErroCheckout;
import com.loja.checkout.service.ServicoResumoCheckout;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

public class ServicoResumoCheckoutTest {
    private ServicoResumoCheckout servico;

    @BeforeEach
    public void setup() {
        servico = new ServicoResumoCheckout();
    }

    @Test
    public void teste1_CamisetaTenisExpressaBemVindo10Pix() {
        RequisicaoResumo requisicao = new RequisicaoResumo();
        requisicao.setItens(Arrays.asList(
            new ItemRequisicao("Camiseta", 79.90, 2, 0.30),
            new ItemRequisicao("Tênis", 249.90, 1, 1.20)
        ));
        requisicao.setModalidadeEntrega("EXPRESSA");
        requisicao.setCupom("BEMVINDO10");
        requisicao.setFormaPagamento("PIX");
        requisicao.setParcelas(1);

        RespostaResumo resposta = servico.calcularResumo(requisicao);

        assertEquals(409.70, resposta.getSubtotalProdutos(), 0.01);
        assertEquals(40.97, resposta.getDescontoCupom(), 0.01);
        assertEquals(33.10, resposta.getFrete(), 0.01);
        assertEquals(2, resposta.getPrazoEntregaDias());
        assertEquals(-20.09, resposta.getAjustePagamento(), 0.01);
        assertEquals(381.74, resposta.getTotalFinal(), 0.01);
        assertEquals(1, resposta.getParcelas());
        assertEquals(381.74, resposta.getValorParcela(), 0.01);
    }

    @Test
    public void teste2_CamisetaTenisEconomicaCartao6x() {
        RequisicaoResumo requisicao = new RequisicaoResumo();
        requisicao.setItens(Arrays.asList(
            new ItemRequisicao("Camiseta", 79.90, 2, 0.30),
            new ItemRequisicao("Tênis", 249.90, 1, 1.20)
        ));
        requisicao.setModalidadeEntrega("ECONOMICA");
        requisicao.setCupom(null);
        requisicao.setFormaPagamento("CARTAO");
        requisicao.setParcelas(6);

        RespostaResumo resposta = servico.calcularResumo(requisicao);

        assertEquals(409.70, resposta.getSubtotalProdutos(), 0.01);
        assertEquals(0.0, resposta.getDescontoCupom(), 0.01);
        assertEquals(15.60, resposta.getFrete(), 0.01);
        assertEquals(7, resposta.getPrazoEntregaDias());
        assertEquals(30.10, resposta.getAjustePagamento(), 0.01);
        assertEquals(455.40, resposta.getTotalFinal(), 0.01);
        assertEquals(6, resposta.getParcelas());
        assertEquals(75.90, resposta.getValorParcela(), 0.01);
    }

    @Test
    public void teste3_FoneMotoboyCupomMenos50Boleto() {
        RequisicaoResumo requisicao = new RequisicaoResumo();
        requisicao.setItens(Arrays.asList(
            new ItemRequisicao("Fone", 199.90, 2, 0.25)
        ));
        requisicao.setModalidadeEntrega("MOTOBOY");
        requisicao.setCupom("MENOS50");
        requisicao.setFormaPagamento("BOLETO");
        requisicao.setParcelas(1);

        RespostaResumo resposta = servico.calcularResumo(requisicao);

        assertEquals(399.80, resposta.getSubtotalProdutos(), 0.01);
        assertEquals(50.00, resposta.getDescontoCupom(), 0.01);
        assertEquals(18.00, resposta.getFrete(), 0.01);
        assertEquals(0, resposta.getPrazoEntregaDias());
        assertEquals(3.49, resposta.getAjustePagamento(), 0.01);
        assertEquals(371.29, resposta.getTotalFinal(), 0.01);
        assertEquals(1, resposta.getParcelas());
        assertEquals(371.29, resposta.getValorParcela(), 0.01);
    }

    @Test
    public void teste4_MeiaCamisetaRetiradaLojaCupomLeve3Pague2Cartao3x() {
        RequisicaoResumo requisicao = new RequisicaoResumo();
        requisicao.setItens(Arrays.asList(
            new ItemRequisicao("Meia", 19.90, 7, 0.10),
            new ItemRequisicao("Camiseta", 79.90, 2, 0.30)
        ));
        requisicao.setModalidadeEntrega("RETIRADA_LOJA");
        requisicao.setCupom("LEVE3PAGUE2");
        requisicao.setFormaPagamento("CARTAO");
        requisicao.setParcelas(3);

        RespostaResumo resposta = servico.calcularResumo(requisicao);

        assertEquals(299.10, resposta.getSubtotalProdutos(), 0.01);
        assertEquals(39.80, resposta.getDescontoCupom(), 0.01);
        assertEquals(0.0, resposta.getFrete(), 0.01);
        assertEquals(1, resposta.getPrazoEntregaDias());
        assertEquals(0.0, resposta.getAjustePagamento(), 0.01);
        assertEquals(259.30, resposta.getTotalFinal(), 0.01);
        assertEquals(3, resposta.getParcelas());
        assertEquals(86.43, resposta.getValorParcela(), 0.01);
    }

    @Test
    public void testeCarrinhoVazio() {
        RequisicaoResumo requisicao = new RequisicaoResumo();
        requisicao.setItens(Arrays.asList());
        requisicao.setModalidadeEntrega("ECONOMICA");
        requisicao.setFormaPagamento("PIX");

        assertThrows(ErroCheckout.class, () -> servico.calcularResumo(requisicao));
    }

    @Test
    public void testePesoNegativo() {
        RequisicaoResumo requisicao = new RequisicaoResumo();
        requisicao.setItens(Arrays.asList(
            new ItemRequisicao("Produto", 50.0, 1, -1.0)
        ));
        requisicao.setModalidadeEntrega("ECONOMICA");
        requisicao.setFormaPagamento("PIX");

        assertThrows(ErroCheckout.class, () -> servico.calcularResumo(requisicao));
    }

    @Test
    public void testeMotoboySobrePeso() {
        RequisicaoResumo requisicao = new RequisicaoResumo();
        requisicao.setItens(Arrays.asList(
            new ItemRequisicao("Produto Pesado", 100.0, 1, 6.0)
        ));
        requisicao.setModalidadeEntrega("MOTOBOY");
        requisicao.setFormaPagamento("PIX");

        assertThrows(ErroCheckout.class, () -> servico.calcularResumo(requisicao));
    }

    @Test
    public void testeCupomInvalido() {
        RequisicaoResumo requisicao = new RequisicaoResumo();
        requisicao.setItens(Arrays.asList(
            new ItemRequisicao("Produto", 50.0, 1, 0.5)
        ));
        requisicao.setModalidadeEntrega("ECONOMICA");
        requisicao.setCupom("CUPOM_INEXISTENTE");
        requisicao.setFormaPagamento("PIX");

        assertThrows(ErroCheckout.class, () -> servico.calcularResumo(requisicao));
    }

    @Test
    public void testeCupomMenos50NaoAplicavel() {
        RequisicaoResumo requisicao = new RequisicaoResumo();
        requisicao.setItens(Arrays.asList(
            new ItemRequisicao("Produto", 100.0, 1, 0.5)
        ));
        requisicao.setModalidadeEntrega("ECONOMICA");
        requisicao.setCupom("MENOS50");
        requisicao.setFormaPagamento("PIX");

        assertThrows(ErroCheckout.class, () -> servico.calcularResumo(requisicao));
    }

    @Test
    public void testeFormaPagamentoInvalida() {
        RequisicaoResumo requisicao = new RequisicaoResumo();
        requisicao.setItens(Arrays.asList(
            new ItemRequisicao("Produto", 50.0, 1, 0.5)
        ));
        requisicao.setModalidadeEntrega("ECONOMICA");
        requisicao.setFormaPagamento("CREDITO");

        assertThrows(ErroCheckout.class, () -> servico.calcularResumo(requisicao));
    }

    @Test
    public void testeParcelasInvalidasPixEBoleto() {
        RequisicaoResumo requisicao = new RequisicaoResumo();
        requisicao.setItens(Arrays.asList(
            new ItemRequisicao("Produto", 50.0, 1, 0.5)
        ));
        requisicao.setModalidadeEntrega("ECONOMICA");
        requisicao.setFormaPagamento("PIX");
        requisicao.setParcelas(2);

        assertThrows(ErroCheckout.class, () -> servico.calcularResumo(requisicao));
    }

    @Test
    public void testeBoletoAcimaDeR1000() {
        RequisicaoResumo requisicao = new RequisicaoResumo();
        requisicao.setItens(Arrays.asList(
            new ItemRequisicao("Produto Caro", 1100.0, 1, 0.5)
        ));
        requisicao.setModalidadeEntrega("RETIRADA_LOJA");
        requisicao.setFormaPagamento("BOLETO");

        assertThrows(ErroCheckout.class, () -> servico.calcularResumo(requisicao));
    }
}
