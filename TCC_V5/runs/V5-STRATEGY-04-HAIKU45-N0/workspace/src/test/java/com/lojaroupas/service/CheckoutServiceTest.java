package com.lojaroupas.service;

import com.lojaroupas.dto.CheckoutRequest;
import com.lojaroupas.dto.CheckoutResponse;
import com.lojaroupas.dto.ItemCarrinho;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

public class CheckoutServiceTest {

    private CheckoutService service;

    @BeforeEach
    public void setUp() {
        service = new CheckoutService();
    }

    @Test
    public void testExemplo1() throws CheckoutException {
        // Camiseta 79,90 × 2 (0,30 kg) + Tênis 249,90 × 1 (1,20 kg), EXPRESSA, BEMVINDO10, PIX, BRONZE, NORTE
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Arrays.asList(
                new ItemCarrinho("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
                new ItemCarrinho("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"))
        ));
        request.setModalidadeEntrega("EXPRESSA");
        request.setCupom("BEMVINDO10");
        request.setFormaPagamento("PIX");
        request.setNivelClube("BRONZE");
        request.setRegiao("NORTE");

        CheckoutResponse response = service.calcularCheckout(request);

        assertEquals(new BigDecimal("409.70"), response.getSubtotalProdutos());
        assertEquals(new BigDecimal("40.97"), response.getDescontoCupom());
        assertEquals(new BigDecimal("33.10"), response.getFrete());
        assertEquals(2, response.getPrazoEntregaDias());
        assertEquals(new BigDecimal("10.24"), response.getSeguro());
        assertEquals(new BigDecimal("-20.60"), response.getAjustePagamento());
        assertEquals(new BigDecimal("391.47"), response.getTotalFinal());
        assertEquals(1, response.getParcelas());
        assertEquals(new BigDecimal("391.47"), response.getValorParcela());
        assertEquals(new BigDecimal("0.00"), response.getCreditoProximaCompra());
        assertFalse(response.getBrinde());
    }

    @Test
    public void testExemplo2() throws CheckoutException {
        // Camiseta 79,90 × 2 (0,30 kg) + Tênis 249,90 × 1 (1,20 kg), ECONOMICA, sem cupom, CARTAO 6×, PRATA, CENTRO_OESTE
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Arrays.asList(
                new ItemCarrinho("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
                new ItemCarrinho("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"))
        ));
        request.setModalidadeEntrega("ECONOMICA");
        request.setFormaPagamento("CARTAO");
        request.setParcelas(6);
        request.setNivelClube("PRATA");
        request.setRegiao("CENTRO_OESTE");

        CheckoutResponse response = service.calcularCheckout(request);

        assertEquals(new BigDecimal("409.70"), response.getSubtotalProdutos());
        assertEquals(new BigDecimal("0.00"), response.getDescontoCupom());
        assertEquals(new BigDecimal("15.60"), response.getFrete());
        assertEquals(7, response.getPrazoEntregaDias());
        assertEquals(new BigDecimal("6.15"), response.getSeguro());
        assertEquals(new BigDecimal("30.55"), response.getAjustePagamento());
        assertEquals(new BigDecimal("462.00"), response.getTotalFinal());
        assertEquals(6, response.getParcelas());
        assertEquals(new BigDecimal("77.00"), response.getValorParcela());
        assertEquals(new BigDecimal("8.19"), response.getCreditoProximaCompra());
        assertFalse(response.getBrinde());
    }

    @Test
    public void testExemplo3() throws CheckoutException {
        // Fone 199,90 × 2 (0,25 kg), MOTOBOY, MENOS50, BOLETO, BRONZE, NORDESTE
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Arrays.asList(
                new ItemCarrinho("Fone", new BigDecimal("199.90"), 2, new BigDecimal("0.25"))
        ));
        request.setModalidadeEntrega("MOTOBOY");
        request.setCupom("MENOS50");
        request.setFormaPagamento("BOLETO");
        request.setNivelClube("BRONZE");
        request.setRegiao("NORDESTE");

        CheckoutResponse response = service.calcularCheckout(request);

        assertEquals(new BigDecimal("399.80"), response.getSubtotalProdutos());
        assertEquals(new BigDecimal("50.00"), response.getDescontoCupom());
        assertEquals(new BigDecimal("18.00"), response.getFrete());
        assertEquals(0, response.getPrazoEntregaDias());
        assertEquals(new BigDecimal("8.00"), response.getSeguro());
        assertEquals(new BigDecimal("3.49"), response.getAjustePagamento());
        assertEquals(new BigDecimal("379.29"), response.getTotalFinal());
        assertEquals(1, response.getParcelas());
        assertEquals(new BigDecimal("379.29"), response.getValorParcela());
        assertEquals(new BigDecimal("0.00"), response.getCreditoProximaCompra());
        assertFalse(response.getBrinde());
    }

    @Test
    public void testExemplo4() throws CheckoutException {
        // Meia 19,90 × 7 (0,10 kg) + Camiseta 79,90 × 2 (0,30 kg), RETIRADA_LOJA, LEVE3PAGUE2, CARTAO 3×, PRATA, SUL
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Arrays.asList(
                new ItemCarrinho("Meia", new BigDecimal("19.90"), 7, new BigDecimal("0.10")),
                new ItemCarrinho("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30"))
        ));
        request.setModalidadeEntrega("RETIRADA_LOJA");
        request.setCupom("LEVE3PAGUE2");
        request.setFormaPagamento("CARTAO");
        request.setParcelas(3);
        request.setNivelClube("PRATA");
        request.setRegiao("SUL");

        CheckoutResponse response = service.calcularCheckout(request);

        assertEquals(new BigDecimal("299.10"), response.getSubtotalProdutos());
        assertEquals(new BigDecimal("39.80"), response.getDescontoCupom());
        assertEquals(new BigDecimal("0.00"), response.getFrete());
        assertEquals(1, response.getPrazoEntregaDias());
        assertEquals(new BigDecimal("2.99"), response.getSeguro());
        assertEquals(new BigDecimal("0.00"), response.getAjustePagamento());
        assertEquals(new BigDecimal("262.29"), response.getTotalFinal());
        assertEquals(3, response.getParcelas());
        assertEquals(new BigDecimal("87.43"), response.getValorParcela());
        assertEquals(new BigDecimal("5.98"), response.getCreditoProximaCompra());
        assertFalse(response.getBrinde());
    }

    @Test
    public void testExemplo5() throws CheckoutException {
        // Camiseta 79,90 × 2 (0,30 kg) + Tênis 249,90 × 1 (1,20 kg), EXPRESSA, sem cupom, PIX, OURO, SUDESTE
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Arrays.asList(
                new ItemCarrinho("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
                new ItemCarrinho("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"))
        ));
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("PIX");
        request.setNivelClube("OURO");
        request.setRegiao("SUDESTE");

        CheckoutResponse response = service.calcularCheckout(request);

        assertEquals(new BigDecimal("409.70"), response.getSubtotalProdutos());
        assertEquals(new BigDecimal("0.00"), response.getDescontoCupom());
        assertEquals(new BigDecimal("0.00"), response.getFrete());
        assertEquals(2, response.getPrazoEntregaDias());
        assertEquals(new BigDecimal("4.10"), response.getSeguro());
        assertEquals(new BigDecimal("-20.69"), response.getAjustePagamento());
        assertEquals(new BigDecimal("393.11"), response.getTotalFinal());
        assertEquals(1, response.getParcelas());
        assertEquals(new BigDecimal("393.11"), response.getValorParcela());
        assertEquals(new BigDecimal("20.48"), response.getCreditoProximaCompra());
        assertFalse(response.getBrinde());
    }

    @Test
    public void testCarrinhoVazio() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Arrays.asList());

        assertThrows(CheckoutException.class, () -> service.calcularCheckout(request));
    }

    @Test
    public void testNivelClubeInvalido() throws CheckoutException {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Arrays.asList(
                new ItemCarrinho("Produto", new BigDecimal("100.00"), 1, new BigDecimal("1.00"))
        ));
        request.setNivelClube("INVALIDO");

        try {
            service.calcularCheckout(request);
            fail("Deveria lançar CheckoutException");
        } catch (CheckoutException e) {
            assertEquals("NIVEL_CLUBE_INVALIDO", e.getCodigo());
        }
    }

    @Test
    public void testMotoboySobreOLimite() throws CheckoutException {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Arrays.asList(
                new ItemCarrinho("Produto Pesado", new BigDecimal("100.00"), 1, new BigDecimal("6.00"))
        ));
        request.setModalidadeEntrega("MOTOBOY");
        request.setNivelClube("BRONZE");
        request.setRegiao("SUDESTE");
        request.setFormaPagamento("PIX");

        try {
            service.calcularCheckout(request);
            fail("Deveria lançar CheckoutException");
        } catch (CheckoutException e) {
            assertEquals("MODALIDADE_INDISPONIVEL", e.getCodigo());
        }
    }

}
