package com.loja.checkout.service;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.loja.checkout.domain.Item;
import com.loja.checkout.dto.ResumoResponse;

public class CalculadoraResumoTest {
    private final CalculadoraResumo calculadora = new CalculadoraResumo();

    private void assertBigDecimalEquals(BigDecimal expected, BigDecimal actual) {
        assertEquals(0, expected.compareTo(actual),
            "Expected " + expected + " but was " + actual);
    }

    @Test
    public void exemplo1() {
        List<Item> itens = Arrays.asList(
            new Item("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
            new Item("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"))
        );

        ResumoResponse resposta = calculadora.calcular(
            itens,
            "EXPRESSA",
            "BEMVINDO10",
            "PIX",
            null,
            "BRONZE",
            "NORTE"
        );

        assertBigDecimalEquals(new BigDecimal("409.70"), resposta.getSubtotalProdutos());
        assertBigDecimalEquals(new BigDecimal("40.97"), resposta.getDescontoCupom());
        assertBigDecimalEquals(new BigDecimal("33.10"), resposta.getFrete());
        assertEquals(2, resposta.getPrazoEntregaDias());
        assertBigDecimalEquals(new BigDecimal("10.24"), resposta.getSeguro());
        assertBigDecimalEquals(new BigDecimal("-20.60"), resposta.getAjustePagamento());
        assertBigDecimalEquals(new BigDecimal("391.47"), resposta.getTotalFinal());
        assertEquals(1, resposta.getParcelas());
        assertBigDecimalEquals(new BigDecimal("391.47"), resposta.getValorParcela());
        assertBigDecimalEquals(new BigDecimal("0.00"), resposta.getCreditoProximaCompra());
        assertFalse(resposta.isBrinde());
    }

    @Test
    public void exemplo2() {
        List<Item> itens = Arrays.asList(
            new Item("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
            new Item("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"))
        );

        ResumoResponse resposta = calculadora.calcular(
            itens,
            "ECONOMICA",
            null,
            "CARTAO",
            6,
            "PRATA",
            "CENTRO_OESTE"
        );

        assertBigDecimalEquals(new BigDecimal("409.70"), resposta.getSubtotalProdutos());
        assertBigDecimalEquals(new BigDecimal("0.00"), resposta.getDescontoCupom());
        assertBigDecimalEquals(new BigDecimal("15.60"), resposta.getFrete());
        assertEquals(7, resposta.getPrazoEntregaDias());
        assertBigDecimalEquals(new BigDecimal("6.15"), resposta.getSeguro());
        assertBigDecimalEquals(new BigDecimal("30.55"), resposta.getAjustePagamento());
        assertBigDecimalEquals(new BigDecimal("462.00"), resposta.getTotalFinal());
        assertEquals(6, resposta.getParcelas());
        assertBigDecimalEquals(new BigDecimal("77.00"), resposta.getValorParcela());
        assertBigDecimalEquals(new BigDecimal("8.19"), resposta.getCreditoProximaCompra());
        assertFalse(resposta.isBrinde());
    }

    @Test
    public void exemplo3() {
        List<Item> itens = Arrays.asList(
            new Item("Fone", new BigDecimal("199.90"), 2, new BigDecimal("0.25"))
        );

        ResumoResponse resposta = calculadora.calcular(
            itens,
            "MOTOBOY",
            "MENOS50",
            "BOLETO",
            null,
            "BRONZE",
            "NORDESTE"
        );

        assertBigDecimalEquals(new BigDecimal("399.80"), resposta.getSubtotalProdutos());
        assertBigDecimalEquals(new BigDecimal("50.00"), resposta.getDescontoCupom());
        assertBigDecimalEquals(new BigDecimal("18.00"), resposta.getFrete());
        assertEquals(0, resposta.getPrazoEntregaDias());
        assertBigDecimalEquals(new BigDecimal("8.00"), resposta.getSeguro());
        assertBigDecimalEquals(new BigDecimal("3.49"), resposta.getAjustePagamento());
        assertBigDecimalEquals(new BigDecimal("379.29"), resposta.getTotalFinal());
        assertEquals(1, resposta.getParcelas());
        assertBigDecimalEquals(new BigDecimal("379.29"), resposta.getValorParcela());
        assertBigDecimalEquals(new BigDecimal("0.00"), resposta.getCreditoProximaCompra());
        assertFalse(resposta.isBrinde());
    }

    @Test
    public void exemplo4() {
        List<Item> itens = Arrays.asList(
            new Item("Meia", new BigDecimal("19.90"), 7, new BigDecimal("0.10")),
            new Item("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30"))
        );

        ResumoResponse resposta = calculadora.calcular(
            itens,
            "RETIRADA_LOJA",
            "LEVE3PAGUE2",
            "CARTAO",
            3,
            "PRATA",
            "SUL"
        );

        assertBigDecimalEquals(new BigDecimal("299.10"), resposta.getSubtotalProdutos());
        assertBigDecimalEquals(new BigDecimal("39.80"), resposta.getDescontoCupom());
        assertBigDecimalEquals(new BigDecimal("0.00"), resposta.getFrete());
        assertEquals(1, resposta.getPrazoEntregaDias());
        assertBigDecimalEquals(new BigDecimal("2.99"), resposta.getSeguro());
        assertBigDecimalEquals(new BigDecimal("0.00"), resposta.getAjustePagamento());
        assertBigDecimalEquals(new BigDecimal("262.29"), resposta.getTotalFinal());
        assertEquals(3, resposta.getParcelas());
        assertBigDecimalEquals(new BigDecimal("87.43"), resposta.getValorParcela());
        assertBigDecimalEquals(new BigDecimal("5.98"), resposta.getCreditoProximaCompra());
        assertFalse(resposta.isBrinde());
    }

    @Test
    public void exemplo5() {
        List<Item> itens = Arrays.asList(
            new Item("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
            new Item("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"))
        );

        ResumoResponse resposta = calculadora.calcular(
            itens,
            "EXPRESSA",
            null,
            "PIX",
            null,
            "OURO",
            "SUDESTE"
        );

        assertBigDecimalEquals(new BigDecimal("409.70"), resposta.getSubtotalProdutos());
        assertBigDecimalEquals(new BigDecimal("0.00"), resposta.getDescontoCupom());
        assertBigDecimalEquals(new BigDecimal("0.00"), resposta.getFrete());
        assertEquals(2, resposta.getPrazoEntregaDias());
        assertBigDecimalEquals(new BigDecimal("4.10"), resposta.getSeguro());
        assertBigDecimalEquals(new BigDecimal("-20.69"), resposta.getAjustePagamento());
        assertBigDecimalEquals(new BigDecimal("393.11"), resposta.getTotalFinal());
        assertEquals(1, resposta.getParcelas());
        assertBigDecimalEquals(new BigDecimal("393.11"), resposta.getValorParcela());
        assertBigDecimalEquals(new BigDecimal("20.48"), resposta.getCreditoProximaCompra());
        assertFalse(resposta.isBrinde());
    }

    @Test
    public void erroCarrinhoVazio() {
        ResumoResponse resposta = calculadora.calcular(
            Arrays.asList(),
            "ECONOMICA",
            null,
            "PIX",
            null,
            "BRONZE",
            "SUDESTE"
        );

        assertEquals("PEDIDO_INVALIDO", resposta.getErro());
    }

    @Test
    public void erroNivelClubeInvalido() {
        List<Item> itens = Arrays.asList(
            new Item("Camiseta", new BigDecimal("79.90"), 1, new BigDecimal("0.30"))
        );

        ResumoResponse resposta = calculadora.calcular(
            itens,
            "ECONOMICA",
            null,
            "PIX",
            null,
            "INVALIDO",
            "SUDESTE"
        );

        assertEquals("NIVEL_CLUBE_INVALIDO", resposta.getErro());
    }

    @Test
    public void erroRegiaoinvalida() {
        List<Item> itens = Arrays.asList(
            new Item("Camiseta", new BigDecimal("79.90"), 1, new BigDecimal("0.30"))
        );

        ResumoResponse resposta = calculadora.calcular(
            itens,
            "ECONOMICA",
            null,
            "PIX",
            null,
            "BRONZE",
            "INVALIDA"
        );

        assertEquals("REGIAO_INVALIDA", resposta.getErro());
    }

    @Test
    public void erroModalidadeInvalida() {
        List<Item> itens = Arrays.asList(
            new Item("Camiseta", new BigDecimal("79.90"), 1, new BigDecimal("0.30"))
        );

        ResumoResponse resposta = calculadora.calcular(
            itens,
            "INVALIDA",
            null,
            "PIX",
            null,
            "BRONZE",
            "SUDESTE"
        );

        assertEquals("MODALIDADE_INVALIDA", resposta.getErro());
    }

    @Test
    public void erroModalidadeIndisponivel() {
        List<Item> itens = Arrays.asList(
            new Item("Camiseta", new BigDecimal("79.90"), 1, new BigDecimal("10.00"))
        );

        ResumoResponse resposta = calculadora.calcular(
            itens,
            "MOTOBOY",
            null,
            "PIX",
            null,
            "BRONZE",
            "SUDESTE"
        );

        assertEquals("MODALIDADE_INDISPONIVEL", resposta.getErro());
    }

    @Test
    public void erroCupomInvalido() {
        List<Item> itens = Arrays.asList(
            new Item("Camiseta", new BigDecimal("79.90"), 1, new BigDecimal("0.30"))
        );

        ResumoResponse resposta = calculadora.calcular(
            itens,
            "ECONOMICA",
            "INVALIDO",
            "PIX",
            null,
            "BRONZE",
            "SUDESTE"
        );

        assertEquals("CUPOM_INVALIDO", resposta.getErro());
    }

    @Test
    public void erroCupomNaoAplicavel() {
        List<Item> itens = Arrays.asList(
            new Item("Camiseta", new BigDecimal("100.00"), 1, new BigDecimal("0.30"))
        );

        ResumoResponse resposta = calculadora.calcular(
            itens,
            "ECONOMICA",
            "MENOS50",
            "PIX",
            null,
            "BRONZE",
            "SUDESTE"
        );

        assertEquals("CUPOM_NAO_APLICAVEL", resposta.getErro());
    }

    @Test
    public void erroFormaPagamentoInvalida() {
        List<Item> itens = Arrays.asList(
            new Item("Camiseta", new BigDecimal("79.90"), 1, new BigDecimal("0.30"))
        );

        ResumoResponse resposta = calculadora.calcular(
            itens,
            "ECONOMICA",
            null,
            "INVALIDA",
            null,
            "BRONZE",
            "SUDESTE"
        );

        assertEquals("FORMA_PAGAMENTO_INVALIDA", resposta.getErro());
    }

    @Test
    public void erroParcelamentoInvalido() {
        List<Item> itens = Arrays.asList(
            new Item("Camiseta", new BigDecimal("79.90"), 1, new BigDecimal("0.30"))
        );

        ResumoResponse resposta = calculadora.calcular(
            itens,
            "ECONOMICA",
            null,
            "PIX",
            2,
            "BRONZE",
            "SUDESTE"
        );

        assertEquals("PARCELAMENTO_INVALIDO", resposta.getErro());
    }

    @Test
    public void erroFormaPagamentoIndisponivel() {
        List<Item> itens = Arrays.asList(
            new Item("Camiseta", new BigDecimal("50.00"), 30, new BigDecimal("0.30"))
        );

        ResumoResponse resposta = calculadora.calcular(
            itens,
            "ECONOMICA",
            null,
            "BOLETO",
            1,
            "BRONZE",
            "SUDESTE"
        );

        assertEquals("FORMA_PAGAMENTO_INDISPONIVEL", resposta.getErro());
    }
}
