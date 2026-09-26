package com.loja;

import com.loja.dto.ItemPedido;
import com.loja.dto.RequisicaoCheckout;
import com.loja.dto.RespostaCheckout;
import com.loja.dto.RespostaErro;
import com.loja.service.CheckoutService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CheckoutServiceTest {
    private CheckoutService checkoutService;

    @BeforeEach
    public void setup() {
        checkoutService = new CheckoutService();
    }

    @Test
    public void testExemplo1() {
        // Camiseta 79,90 × 2 (0,30 kg) + Tênis 249,90 × 1 (1,20 kg), EXPRESSA, BEMVINDO10, PIX
        List<ItemPedido> itens = new ArrayList<>();
        itens.add(new ItemPedido("Camiseta", 79.90, 2, 0.30));
        itens.add(new ItemPedido("Tênis", 249.90, 1, 1.20));

        RequisicaoCheckout requisicao = new RequisicaoCheckout();
        requisicao.setItens(itens);
        requisicao.setModalidadeEntrega("EXPRESSA");
        requisicao.setCupom("BEMVINDO10");
        requisicao.setFormaPagamento("PIX");
        requisicao.setParcelas(1);
        requisicao.setNivelClube("BRONZE");

        Object resultado = checkoutService.processarCheckout(requisicao);
        assertTrue(resultado instanceof RespostaCheckout);

        RespostaCheckout resposta = (RespostaCheckout) resultado;
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
    public void testExemplo2() {
        // Camiseta 79,90 × 2 (0,30 kg) + Tênis 249,90 × 1 (1,20 kg), ECONOMICA, sem cupom, CARTAO 6×
        List<ItemPedido> itens = new ArrayList<>();
        itens.add(new ItemPedido("Camiseta", 79.90, 2, 0.30));
        itens.add(new ItemPedido("Tênis", 249.90, 1, 1.20));

        RequisicaoCheckout requisicao = new RequisicaoCheckout();
        requisicao.setItens(itens);
        requisicao.setModalidadeEntrega("ECONOMICA");
        requisicao.setCupom(null);
        requisicao.setFormaPagamento("CARTAO");
        requisicao.setParcelas(6);
        requisicao.setNivelClube("BRONZE");

        Object resultado = checkoutService.processarCheckout(requisicao);
        assertTrue(resultado instanceof RespostaCheckout);

        RespostaCheckout resposta = (RespostaCheckout) resultado;
        assertEquals(new BigDecimal("409.70"), resposta.getSubtotalProdutos());
        assertEquals(0, resposta.getDescontoCupom().compareTo(new BigDecimal("0.00")));
        assertEquals(new BigDecimal("15.60"), resposta.getFrete());
        assertEquals(7, resposta.getPrazoEntregaDias());
        assertEquals(new BigDecimal("30.10"), resposta.getAjustePagamento());
        assertEquals(new BigDecimal("455.40"), resposta.getTotalFinal());
        assertEquals(6, resposta.getParcelas());
        assertEquals(new BigDecimal("75.90"), resposta.getValorParcela());
    }

    @Test
    public void testExemplo3() {
        // Fone 199,90 × 2 (0,25 kg), MOTOBOY, MENOS50, BOLETO
        List<ItemPedido> itens = new ArrayList<>();
        itens.add(new ItemPedido("Fone", 199.90, 2, 0.25));

        RequisicaoCheckout requisicao = new RequisicaoCheckout();
        requisicao.setItens(itens);
        requisicao.setModalidadeEntrega("MOTOBOY");
        requisicao.setCupom("MENOS50");
        requisicao.setFormaPagamento("BOLETO");
        requisicao.setParcelas(1);
        requisicao.setNivelClube("BRONZE");

        Object resultado = checkoutService.processarCheckout(requisicao);
        assertTrue(resultado instanceof RespostaCheckout);

        RespostaCheckout resposta = (RespostaCheckout) resultado;
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
    public void testExemplo4() {
        // Meia 19,90 × 7 (0,10 kg) + Camiseta 79,90 × 2 (0,30 kg), RETIRADA_LOJA, LEVE3PAGUE2, CARTAO 3×
        List<ItemPedido> itens = new ArrayList<>();
        itens.add(new ItemPedido("Meia", 19.90, 7, 0.10));
        itens.add(new ItemPedido("Camiseta", 79.90, 2, 0.30));

        RequisicaoCheckout requisicao = new RequisicaoCheckout();
        requisicao.setItens(itens);
        requisicao.setModalidadeEntrega("RETIRADA_LOJA");
        requisicao.setCupom("LEVE3PAGUE2");
        requisicao.setFormaPagamento("CARTAO");
        requisicao.setParcelas(3);
        requisicao.setNivelClube("BRONZE");

        Object resultado = checkoutService.processarCheckout(requisicao);
        assertTrue(resultado instanceof RespostaCheckout);

        RespostaCheckout resposta = (RespostaCheckout) resultado;
        assertEquals(new BigDecimal("299.10"), resposta.getSubtotalProdutos());
        assertEquals(new BigDecimal("39.80"), resposta.getDescontoCupom());
        assertEquals(0, resposta.getFrete().compareTo(new BigDecimal("0.00")));
        assertEquals(1, resposta.getPrazoEntregaDias());
        assertEquals(0, resposta.getAjustePagamento().compareTo(new BigDecimal("0.00")));
        assertEquals(new BigDecimal("259.30"), resposta.getTotalFinal());
        assertEquals(3, resposta.getParcelas());
        assertEquals(new BigDecimal("86.43"), resposta.getValorParcela());
    }

    @Test
    public void testExemplo5() {
        // Camiseta 79,90 × 2 (0,30 kg) + Tênis 249,90 × 1 (1,20 kg), EXPRESSA, sem cupom, PIX, OURO, SUDESTE
        List<ItemPedido> itens = new ArrayList<>();
        itens.add(new ItemPedido("Camiseta", 79.90, 2, 0.30));
        itens.add(new ItemPedido("Tênis", 249.90, 1, 1.20));

        RequisicaoCheckout requisicao = new RequisicaoCheckout();
        requisicao.setItens(itens);
        requisicao.setModalidadeEntrega("EXPRESSA");
        requisicao.setCupom(null);
        requisicao.setFormaPagamento("PIX");
        requisicao.setParcelas(1);
        requisicao.setNivelClube("OURO");
        requisicao.setRegiao("SUDESTE");

        Object resultado = checkoutService.processarCheckout(requisicao);
        assertTrue(resultado instanceof RespostaCheckout);

        RespostaCheckout resposta = (RespostaCheckout) resultado;
        assertEquals(new BigDecimal("409.70"), resposta.getSubtotalProdutos());
        assertEquals(0, resposta.getDescontoCupom().compareTo(new BigDecimal("0.00")));
        assertEquals(0, resposta.getFrete().compareTo(new BigDecimal("0.00")));
        assertEquals(2, resposta.getPrazoEntregaDias());
        assertEquals(new BigDecimal("49.16"), resposta.getImposto());
        assertEquals(new BigDecimal("-22.94"), resposta.getAjustePagamento());
        assertEquals(new BigDecimal("435.92"), resposta.getTotalFinal());
        assertEquals(1, resposta.getParcelas());
        assertEquals(new BigDecimal("435.92"), resposta.getValorParcela());
        assertEquals(new BigDecimal("20.48"), resposta.getCreditoProximaCompra());
        assertEquals(false, resposta.getBrinde());
    }

    @Test
    public void testCarrinhVazio() {
        RequisicaoCheckout requisicao = new RequisicaoCheckout();
        requisicao.setItens(new ArrayList<>());
        requisicao.setModalidadeEntrega("EXPRESSA");
        requisicao.setFormaPagamento("PIX");
        requisicao.setNivelClube("BRONZE");
        requisicao.setRegiao("SUDESTE");

        Object resultado = checkoutService.processarCheckout(requisicao);
        assertTrue(resultado instanceof RespostaErro);
        assertEquals("PEDIDO_INVALIDO", ((RespostaErro) resultado).getErro());
    }

    @Test
    public void testMotoboySobre5kg() {
        List<ItemPedido> itens = new ArrayList<>();
        itens.add(new ItemPedido("Produto Pesado", 100.0, 1, 5.5));

        RequisicaoCheckout requisicao = new RequisicaoCheckout();
        requisicao.setItens(itens);
        requisicao.setModalidadeEntrega("MOTOBOY");
        requisicao.setFormaPagamento("PIX");
        requisicao.setNivelClube("BRONZE");
        requisicao.setRegiao("SUDESTE");

        Object resultado = checkoutService.processarCheckout(requisicao);
        assertTrue(resultado instanceof RespostaErro);
        assertEquals("MODALIDADE_INDISPONIVEL", ((RespostaErro) resultado).getErro());
    }

    @Test
    public void testCupomMenos50SemMinimo() {
        List<ItemPedido> itens = new ArrayList<>();
        itens.add(new ItemPedido("Produto", 100.0, 1, 0.1));

        RequisicaoCheckout requisicao = new RequisicaoCheckout();
        requisicao.setItens(itens);
        requisicao.setModalidadeEntrega("RETIRADA_LOJA");
        requisicao.setCupom("MENOS50");
        requisicao.setFormaPagamento("PIX");
        requisicao.setNivelClube("BRONZE");
        requisicao.setRegiao("SUDESTE");

        Object resultado = checkoutService.processarCheckout(requisicao);
        assertTrue(resultado instanceof RespostaErro);
        assertEquals("CUPOM_NAO_APLICAVEL", ((RespostaErro) resultado).getErro());
    }
}
