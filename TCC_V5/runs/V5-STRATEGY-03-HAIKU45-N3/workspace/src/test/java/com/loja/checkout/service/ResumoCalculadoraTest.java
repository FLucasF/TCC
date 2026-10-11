package com.loja.checkout.service;

import com.loja.checkout.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ResumoCalculadoraTest {

    private ResumoCalculadora calculadora;

    @BeforeEach
    public void setUp() {
        calculadora = new ResumoCalculadora();
    }

    @Test
    public void testExemplo1() throws ValidacaoException {
        RequisicaoResumo req = new RequisicaoResumo();
        List<Item> itens = new ArrayList<>();
        itens.add(new Item("Camiseta", 79.90, 2, 0.30));
        itens.add(new Item("Tênis", 249.90, 1, 1.20));

        req.setItens(itens);
        req.setModalidadeEntrega("EXPRESSA");
        req.setCupom("BEMVINDO10");
        req.setFormaPagamento("PIX");
        req.setNivelClube("BRONZE");
        req.setRegiao("NORTE");

        RespostaResumo resp = calculadora.calcular(req);

        assertEquals(new BigDecimal("409.70"), resp.getSubtotalProdutos());
        assertEquals(new BigDecimal("40.97"), resp.getDescontoCupom());
        assertEquals(new BigDecimal("33.10"), resp.getFrete());
        assertEquals(2, resp.getPrazoEntregaDias());
        assertEquals(new BigDecimal("10.24"), resp.getSeguro());
        assertEquals(new BigDecimal("-20.60"), resp.getAjustePagamento());
        assertEquals(new BigDecimal("391.47"), resp.getTotalFinal());
        assertEquals(1, resp.getParcelas());
        assertEquals(new BigDecimal("391.47"), resp.getValorParcela());
        assertEquals(new BigDecimal("0.00"), resp.getCreditoProximaCompra());
        assertEquals(false, resp.getBrinde());
    }

    @Test
    public void testExemplo2() throws ValidacaoException {
        RequisicaoResumo req = new RequisicaoResumo();
        List<Item> itens = new ArrayList<>();
        itens.add(new Item("Camiseta", 79.90, 2, 0.30));
        itens.add(new Item("Tênis", 249.90, 1, 1.20));

        req.setItens(itens);
        req.setModalidadeEntrega("ECONOMICA");
        req.setFormaPagamento("CARTAO");
        req.setParcelas(6);
        req.setNivelClube("PRATA");
        req.setRegiao("CENTRO_OESTE");

        RespostaResumo resp = calculadora.calcular(req);

        assertEquals(new BigDecimal("409.70"), resp.getSubtotalProdutos());
        assertEquals(new BigDecimal("0.00"), resp.getDescontoCupom());
        assertEquals(new BigDecimal("15.60"), resp.getFrete());
        assertEquals(7, resp.getPrazoEntregaDias());
        assertEquals(new BigDecimal("6.15"), resp.getSeguro());
        assertEquals(new BigDecimal("30.55"), resp.getAjustePagamento());
        assertEquals(new BigDecimal("462.00"), resp.getTotalFinal());
        assertEquals(6, resp.getParcelas());
        assertEquals(new BigDecimal("77.00"), resp.getValorParcela());
        assertEquals(new BigDecimal("8.19"), resp.getCreditoProximaCompra());
        assertEquals(false, resp.getBrinde());
    }

    @Test
    public void testExemplo3() throws ValidacaoException {
        RequisicaoResumo req = new RequisicaoResumo();
        List<Item> itens = new ArrayList<>();
        itens.add(new Item("Fone", 199.90, 2, 0.25));

        req.setItens(itens);
        req.setModalidadeEntrega("MOTOBOY");
        req.setCupom("MENOS50");
        req.setFormaPagamento("BOLETO");
        req.setNivelClube("BRONZE");
        req.setRegiao("NORDESTE");

        RespostaResumo resp = calculadora.calcular(req);

        assertEquals(new BigDecimal("399.80"), resp.getSubtotalProdutos());
        assertEquals(new BigDecimal("50.00"), resp.getDescontoCupom());
        assertEquals(new BigDecimal("18.00"), resp.getFrete());
        assertEquals(0, resp.getPrazoEntregaDias());
        assertEquals(new BigDecimal("8.00"), resp.getSeguro());
        assertEquals(new BigDecimal("3.49"), resp.getAjustePagamento());
        assertEquals(new BigDecimal("379.29"), resp.getTotalFinal());
        assertEquals(1, resp.getParcelas());
        assertEquals(new BigDecimal("379.29"), resp.getValorParcela());
        assertEquals(new BigDecimal("0.00"), resp.getCreditoProximaCompra());
        assertEquals(false, resp.getBrinde());
    }

    @Test
    public void testExemplo4() throws ValidacaoException {
        RequisicaoResumo req = new RequisicaoResumo();
        List<Item> itens = new ArrayList<>();
        itens.add(new Item("Meia", 19.90, 7, 0.10));
        itens.add(new Item("Camiseta", 79.90, 2, 0.30));

        req.setItens(itens);
        req.setModalidadeEntrega("RETIRADA_LOJA");
        req.setCupom("LEVE3PAGUE2");
        req.setFormaPagamento("CARTAO");
        req.setParcelas(3);
        req.setNivelClube("PRATA");
        req.setRegiao("SUL");

        RespostaResumo resp = calculadora.calcular(req);

        assertEquals(new BigDecimal("299.10"), resp.getSubtotalProdutos());
        assertEquals(new BigDecimal("39.80"), resp.getDescontoCupom());
        assertEquals(new BigDecimal("0.00"), resp.getFrete());
        assertEquals(1, resp.getPrazoEntregaDias());
        assertEquals(new BigDecimal("2.99"), resp.getSeguro());
        assertEquals(new BigDecimal("0.00"), resp.getAjustePagamento());
        assertEquals(new BigDecimal("262.29"), resp.getTotalFinal());
        assertEquals(3, resp.getParcelas());
        assertEquals(new BigDecimal("87.43"), resp.getValorParcela());
        assertEquals(new BigDecimal("5.98"), resp.getCreditoProximaCompra());
        assertEquals(false, resp.getBrinde());
    }

    @Test
    public void testExemplo5() throws ValidacaoException {
        RequisicaoResumo req = new RequisicaoResumo();
        List<Item> itens = new ArrayList<>();
        itens.add(new Item("Camiseta", 79.90, 2, 0.30));
        itens.add(new Item("Tênis", 249.90, 1, 1.20));

        req.setItens(itens);
        req.setModalidadeEntrega("EXPRESSA");
        req.setFormaPagamento("PIX");
        req.setNivelClube("OURO");
        req.setRegiao("SUDESTE");

        RespostaResumo resp = calculadora.calcular(req);

        assertEquals(new BigDecimal("409.70"), resp.getSubtotalProdutos());
        assertEquals(new BigDecimal("0.00"), resp.getDescontoCupom());
        assertEquals(new BigDecimal("0.00"), resp.getFrete());
        assertEquals(2, resp.getPrazoEntregaDias());
        assertEquals(new BigDecimal("4.10"), resp.getSeguro());
        assertEquals(new BigDecimal("-20.69"), resp.getAjustePagamento());
        assertEquals(new BigDecimal("393.11"), resp.getTotalFinal());
        assertEquals(1, resp.getParcelas());
        assertEquals(new BigDecimal("393.11"), resp.getValorParcela());
        assertEquals(new BigDecimal("20.48"), resp.getCreditoProximaCompra());
        assertEquals(false, resp.getBrinde());
    }
}
