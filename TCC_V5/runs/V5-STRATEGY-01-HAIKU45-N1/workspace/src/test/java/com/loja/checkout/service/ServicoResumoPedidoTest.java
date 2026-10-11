package com.loja.checkout.service;

import com.loja.checkout.dto.ItemPedido;
import com.loja.checkout.dto.RequisicaoPedido;
import com.loja.checkout.dto.RespostaPedido;
import com.loja.checkout.dto.RespostaErro;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

public class ServicoResumoPedidoTest {
    private ServicoResumoPedido servico;

    @BeforeEach
    public void setup() {
        servico = new ServicoResumoPedido();
    }

    @Test
    public void exemplo1() {
        RequisicaoPedido req = new RequisicaoPedido();
        req.setItens(Arrays.asList(
            new ItemPedido("Camiseta", 79.90, 2, 0.30),
            new ItemPedido("Tênis", 249.90, 1, 1.20)
        ));
        req.setModalidadeEntrega("EXPRESSA");
        req.setCupom("BEMVINDO10");
        req.setFormaPagamento("PIX");
        req.setParcelas(1);
        req.setNivelClube("BRONZE");
        req.setRegiao("NORTE");

        Object resultado = servico.calcularResumo(req);
        assertTrue(resultado instanceof RespostaPedido);

        RespostaPedido resp = (RespostaPedido) resultado;
        assertEquals(409.70, resp.getSubtotalProdutos());
        assertEquals(40.97, resp.getDescontoCupom());
        assertEquals(33.10, resp.getFrete());
        assertEquals(2, resp.getPrazoEntregaDias());
        assertEquals(10.24, resp.getSeguro());
        assertEquals(-20.60, resp.getAjustePagamento());
        assertEquals(391.47, resp.getTotalFinal());
        assertEquals(1, resp.getParcelas());
        assertEquals(391.47, resp.getValorParcela());
        assertEquals(0.00, resp.getCreditoProximaCompra());
        assertEquals(false, resp.getBrinde());
    }

    @Test
    public void exemplo2() {
        RequisicaoPedido req = new RequisicaoPedido();
        req.setItens(Arrays.asList(
            new ItemPedido("Camiseta", 79.90, 2, 0.30),
            new ItemPedido("Tênis", 249.90, 1, 1.20)
        ));
        req.setModalidadeEntrega("ECONOMICA");
        req.setCupom(null);
        req.setFormaPagamento("CARTAO");
        req.setParcelas(6);
        req.setNivelClube("PRATA");
        req.setRegiao("CENTRO_OESTE");

        Object resultado = servico.calcularResumo(req);
        assertTrue(resultado instanceof RespostaPedido);

        RespostaPedido resp = (RespostaPedido) resultado;
        assertEquals(409.70, resp.getSubtotalProdutos());
        assertEquals(0.00, resp.getDescontoCupom());
        assertEquals(15.60, resp.getFrete());
        assertEquals(7, resp.getPrazoEntregaDias());
        assertEquals(6.15, resp.getSeguro());
        assertEquals(30.55, resp.getAjustePagamento());
        assertEquals(462.00, resp.getTotalFinal());
        assertEquals(6, resp.getParcelas());
        assertEquals(77.00, resp.getValorParcela());
        assertEquals(8.19, resp.getCreditoProximaCompra());
        assertEquals(false, resp.getBrinde());
    }

    @Test
    public void exemplo3() {
        RequisicaoPedido req = new RequisicaoPedido();
        req.setItens(Arrays.asList(
            new ItemPedido("Fone", 199.90, 2, 0.25)
        ));
        req.setModalidadeEntrega("MOTOBOY");
        req.setCupom("MENOS50");
        req.setFormaPagamento("BOLETO");
        req.setParcelas(1);
        req.setNivelClube("BRONZE");
        req.setRegiao("NORDESTE");

        Object resultado = servico.calcularResumo(req);
        assertTrue(resultado instanceof RespostaPedido);

        RespostaPedido resp = (RespostaPedido) resultado;
        assertEquals(399.80, resp.getSubtotalProdutos());
        assertEquals(50.00, resp.getDescontoCupom());
        assertEquals(18.00, resp.getFrete());
        assertEquals(0, resp.getPrazoEntregaDias());
        assertEquals(8.00, resp.getSeguro());
        assertEquals(3.49, resp.getAjustePagamento());
        assertEquals(379.29, resp.getTotalFinal());
        assertEquals(1, resp.getParcelas());
        assertEquals(379.29, resp.getValorParcela());
        assertEquals(0.00, resp.getCreditoProximaCompra());
        assertEquals(false, resp.getBrinde());
    }

    @Test
    public void exemplo4() {
        RequisicaoPedido req = new RequisicaoPedido();
        req.setItens(Arrays.asList(
            new ItemPedido("Meia", 19.90, 7, 0.10),
            new ItemPedido("Camiseta", 79.90, 2, 0.30)
        ));
        req.setModalidadeEntrega("RETIRADA_LOJA");
        req.setCupom("LEVE3PAGUE2");
        req.setFormaPagamento("CARTAO");
        req.setParcelas(3);
        req.setNivelClube("PRATA");
        req.setRegiao("SUL");

        Object resultado = servico.calcularResumo(req);
        assertTrue(resultado instanceof RespostaPedido);

        RespostaPedido resp = (RespostaPedido) resultado;
        assertEquals(299.10, resp.getSubtotalProdutos());
        assertEquals(39.80, resp.getDescontoCupom());
        assertEquals(0.00, resp.getFrete());
        assertEquals(1, resp.getPrazoEntregaDias());
        assertEquals(2.99, resp.getSeguro());
        assertEquals(0.00, resp.getAjustePagamento());
        assertEquals(262.29, resp.getTotalFinal());
        assertEquals(3, resp.getParcelas());
        assertEquals(87.43, resp.getValorParcela());
        assertEquals(5.98, resp.getCreditoProximaCompra());
        assertEquals(false, resp.getBrinde());
    }

    @Test
    public void exemplo5() {
        RequisicaoPedido req = new RequisicaoPedido();
        req.setItens(Arrays.asList(
            new ItemPedido("Camiseta", 79.90, 2, 0.30),
            new ItemPedido("Tênis", 249.90, 1, 1.20)
        ));
        req.setModalidadeEntrega("EXPRESSA");
        req.setCupom(null);
        req.setFormaPagamento("PIX");
        req.setParcelas(1);
        req.setNivelClube("OURO");
        req.setRegiao("SUDESTE");

        Object resultado = servico.calcularResumo(req);
        assertTrue(resultado instanceof RespostaPedido);

        RespostaPedido resp = (RespostaPedido) resultado;
        assertEquals(409.70, resp.getSubtotalProdutos());
        assertEquals(0.00, resp.getDescontoCupom());
        assertEquals(0.00, resp.getFrete());
        assertEquals(2, resp.getPrazoEntregaDias());
        assertEquals(4.10, resp.getSeguro());
        assertEquals(-20.69, resp.getAjustePagamento());
        assertEquals(393.11, resp.getTotalFinal());
        assertEquals(1, resp.getParcelas());
        assertEquals(393.11, resp.getValorParcela());
        assertEquals(20.48, resp.getCreditoProximaCompra());
        assertEquals(false, resp.getBrinde());
    }

    @Test
    public void pedidoInvalido() {
        RequisicaoPedido req = new RequisicaoPedido();
        req.setItens(Arrays.asList());
        req.setModalidadeEntrega("EXPRESSA");
        req.setFormaPagamento("PIX");
        req.setNivelClube("BRONZE");
        req.setRegiao("NORTE");

        Object resultado = servico.calcularResumo(req);
        assertTrue(resultado instanceof RespostaErro);
        assertEquals("PEDIDO_INVALIDO", ((RespostaErro) resultado).getErro());
    }

    @Test
    public void nivelInvalido() {
        RequisicaoPedido req = new RequisicaoPedido();
        req.setItens(Arrays.asList(
            new ItemPedido("Teste", 10.0, 1, 0.1)
        ));
        req.setModalidadeEntrega("EXPRESSA");
        req.setFormaPagamento("PIX");
        req.setNivelClube("INVALIDO");
        req.setRegiao("NORTE");

        Object resultado = servico.calcularResumo(req);
        assertTrue(resultado instanceof RespostaErro);
        assertEquals("NIVEL_CLUBE_INVALIDO", ((RespostaErro) resultado).getErro());
    }

    @Test
    public void modalidadeIndisponivel() {
        RequisicaoPedido req = new RequisicaoPedido();
        req.setItens(Arrays.asList(
            new ItemPedido("Teste", 10.0, 100, 0.1)
        ));
        req.setModalidadeEntrega("MOTOBOY");
        req.setFormaPagamento("PIX");
        req.setNivelClube("BRONZE");
        req.setRegiao("NORTE");

        Object resultado = servico.calcularResumo(req);
        assertTrue(resultado instanceof RespostaErro);
        assertEquals("MODALIDADE_INDISPONIVEL", ((RespostaErro) resultado).getErro());
    }

    @Test
    public void cupomInvalido() {
        RequisicaoPedido req = new RequisicaoPedido();
        req.setItens(Arrays.asList(
            new ItemPedido("Teste", 10.0, 1, 0.1)
        ));
        req.setModalidadeEntrega("EXPRESSA");
        req.setCupom("CUPOMINVALIDO");
        req.setFormaPagamento("PIX");
        req.setNivelClube("BRONZE");
        req.setRegiao("NORTE");

        Object resultado = servico.calcularResumo(req);
        assertTrue(resultado instanceof RespostaErro);
        assertEquals("CUPOM_INVALIDO", ((RespostaErro) resultado).getErro());
    }

    @Test
    public void cupomNaoAplicavel() {
        RequisicaoPedido req = new RequisicaoPedido();
        req.setItens(Arrays.asList(
            new ItemPedido("Teste", 10.0, 1, 0.1)
        ));
        req.setModalidadeEntrega("EXPRESSA");
        req.setCupom("MENOS50");
        req.setFormaPagamento("PIX");
        req.setNivelClube("BRONZE");
        req.setRegiao("NORTE");

        Object resultado = servico.calcularResumo(req);
        assertTrue(resultado instanceof RespostaErro);
        assertEquals("CUPOM_NAO_APLICAVEL", ((RespostaErro) resultado).getErro());
    }

    @Test
    public void parcelamentoInvalido() {
        RequisicaoPedido req = new RequisicaoPedido();
        req.setItens(Arrays.asList(
            new ItemPedido("Teste", 10.0, 1, 0.1)
        ));
        req.setModalidadeEntrega("EXPRESSA");
        req.setFormaPagamento("PIX");
        req.setParcelas(2);
        req.setNivelClube("BRONZE");
        req.setRegiao("NORTE");

        Object resultado = servico.calcularResumo(req);
        assertTrue(resultado instanceof RespostaErro);
        assertEquals("PARCELAMENTO_INVALIDO", ((RespostaErro) resultado).getErro());
    }

    @Test
    public void formaPagamentoIndisponivel() {
        RequisicaoPedido req = new RequisicaoPedido();
        req.setItens(Arrays.asList(
            new ItemPedido("Teste", 1001.0, 1, 0.1)
        ));
        req.setModalidadeEntrega("EXPRESSA");
        req.setFormaPagamento("BOLETO");
        req.setParcelas(1);
        req.setNivelClube("BRONZE");
        req.setRegiao("NORTE");

        Object resultado = servico.calcularResumo(req);
        assertTrue(resultado instanceof RespostaErro);
        assertEquals("FORMA_PAGAMENTO_INDISPONIVEL", ((RespostaErro) resultado).getErro());
    }
}
