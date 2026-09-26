package com.loja.service;

import com.loja.dto.CheckoutRequestDTO;
import com.loja.dto.CheckoutResponseDTO;
import com.loja.dto.ItemPedidoDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

public class CheckoutServiceTest {

    private CheckoutService checkoutService;

    @BeforeEach
    public void setup() {
        checkoutService = new CheckoutService();
    }

    @Test
    public void testeExemplo1_CamisetaTenisExpressaBemvindo10Pix() {
        CheckoutRequestDTO request = new CheckoutRequestDTO();
        request.setItens(Arrays.asList(
            new ItemPedidoDTO("Camiseta", 79.90, 2, 0.30),
            new ItemPedidoDTO("Tênis", 249.90, 1, 1.20)
        ));
        request.setModalidadeEntrega("EXPRESSA");
        request.setCupom("BEMVINDO10");
        request.setFormaPagamento("PIX");
        request.setParcelas(1);

        CheckoutResponseDTO response = checkoutService.calcularResumo(request);

        assertEquals(new BigDecimal("409.70"), response.getSubtotalProdutos());
        assertEquals(new BigDecimal("40.97"), response.getDescontoCupom());
        assertEquals(new BigDecimal("33.10"), response.getFrete());
        assertEquals(2, response.getPrazoEntregaDias());
        assertEquals(new BigDecimal("-20.09"), response.getAjustePagamento());
        assertEquals(new BigDecimal("381.74"), response.getTotalFinal());
        assertEquals(1, response.getParcelas());
        assertEquals(new BigDecimal("381.74"), response.getValorParcela());
    }

    @Test
    public void testeExemplo2_CamisetaTenisEconomicaSemCupomCartao6x() {
        CheckoutRequestDTO request = new CheckoutRequestDTO();
        request.setItens(Arrays.asList(
            new ItemPedidoDTO("Camiseta", 79.90, 2, 0.30),
            new ItemPedidoDTO("Tênis", 249.90, 1, 1.20)
        ));
        request.setModalidadeEntrega("ECONOMICA");
        request.setFormaPagamento("CARTAO");
        request.setParcelas(6);

        CheckoutResponseDTO response = checkoutService.calcularResumo(request);

        assertEquals(new BigDecimal("409.70"), response.getSubtotalProdutos());
        assertEquals(new BigDecimal("0.00"), response.getDescontoCupom());
        assertEquals(new BigDecimal("15.60"), response.getFrete());
        assertEquals(7, response.getPrazoEntregaDias());
        assertEquals(new BigDecimal("30.10"), response.getAjustePagamento());
        assertEquals(new BigDecimal("455.40"), response.getTotalFinal());
        assertEquals(6, response.getParcelas());
        assertEquals(new BigDecimal("75.90"), response.getValorParcela());
    }

    @Test
    public void testeExemplo3_FoneMotoboy2xMenos50Boleto() {
        CheckoutRequestDTO request = new CheckoutRequestDTO();
        request.setItens(Collections.singletonList(
            new ItemPedidoDTO("Fone", 199.90, 2, 0.25)
        ));
        request.setModalidadeEntrega("MOTOBOY");
        request.setCupom("MENOS50");
        request.setFormaPagamento("BOLETO");
        request.setParcelas(1);

        CheckoutResponseDTO response = checkoutService.calcularResumo(request);

        assertEquals(new BigDecimal("399.80"), response.getSubtotalProdutos());
        assertEquals(new BigDecimal("50.00"), response.getDescontoCupom());
        assertEquals(new BigDecimal("18.00"), response.getFrete());
        assertEquals(0, response.getPrazoEntregaDias());
        assertEquals(new BigDecimal("3.49"), response.getAjustePagamento());
        assertEquals(new BigDecimal("371.29"), response.getTotalFinal());
        assertEquals(1, response.getParcelas());
        assertEquals(new BigDecimal("371.29"), response.getValorParcela());
    }

    @Test
    public void testeExemplo4_MeiasCamisetasRetiraLojaLeve3Pague2Cartao3x() {
        CheckoutRequestDTO request = new CheckoutRequestDTO();
        request.setItens(Arrays.asList(
            new ItemPedidoDTO("Meia", 19.90, 7, 0.10),
            new ItemPedidoDTO("Camiseta", 79.90, 2, 0.30)
        ));
        request.setModalidadeEntrega("RETIRADA_LOJA");
        request.setCupom("LEVE3PAGUE2");
        request.setFormaPagamento("CARTAO");
        request.setParcelas(3);

        CheckoutResponseDTO response = checkoutService.calcularResumo(request);

        assertEquals(new BigDecimal("299.10"), response.getSubtotalProdutos());
        assertEquals(new BigDecimal("39.80"), response.getDescontoCupom());
        assertEquals(new BigDecimal("0.00"), response.getFrete());
        assertEquals(1, response.getPrazoEntregaDias());
        assertEquals(BigDecimal.ZERO.setScale(2, java.math.RoundingMode.HALF_EVEN), response.getAjustePagamento());
        assertEquals(new BigDecimal("259.30"), response.getTotalFinal());
        assertEquals(3, response.getParcelas());
        assertEquals(new BigDecimal("86.43"), response.getValorParcela());
    }

    @Test
    public void testeErro_CarrinhoVazio() {
        CheckoutRequestDTO request = new CheckoutRequestDTO();
        request.setItens(Collections.emptyList());

        assertThrows(ErroCheckout.class, () -> checkoutService.calcularResumo(request));
    }

    @Test
    public void testeErro_ModalidadeInvalida() {
        CheckoutRequestDTO request = new CheckoutRequestDTO();
        request.setItens(Collections.singletonList(
            new ItemPedidoDTO("Produto", 100.0, 1, 1.0)
        ));
        request.setModalidadeEntrega("INVALIDA");
        request.setFormaPagamento("PIX");

        assertThrows(ErroCheckout.class, () -> checkoutService.calcularResumo(request));
    }

    @Test
    public void testeErro_ModalidadeIndisponivel_MotoboySobrepesoMaisDe5Kg() {
        CheckoutRequestDTO request = new CheckoutRequestDTO();
        request.setItens(Collections.singletonList(
            new ItemPedidoDTO("Produto", 100.0, 6, 1.0)
        ));
        request.setModalidadeEntrega("MOTOBOY");
        request.setFormaPagamento("PIX");

        assertThrows(ErroCheckout.class, () -> checkoutService.calcularResumo(request));
    }

    @Test
    public void testeErro_CupomInvalido() {
        CheckoutRequestDTO request = new CheckoutRequestDTO();
        request.setItens(Collections.singletonList(
            new ItemPedidoDTO("Produto", 100.0, 1, 1.0)
        ));
        request.setModalidadeEntrega("ECONOMICA");
        request.setCupom("INVALIDO");
        request.setFormaPagamento("PIX");

        assertThrows(ErroCheckout.class, () -> checkoutService.calcularResumo(request));
    }

    @Test
    public void testeErro_CupomNaoAplicavel_Menos50AbaIxo300() {
        CheckoutRequestDTO request = new CheckoutRequestDTO();
        request.setItens(Collections.singletonList(
            new ItemPedidoDTO("Produto", 100.0, 1, 1.0)
        ));
        request.setModalidadeEntrega("ECONOMICA");
        request.setCupom("MENOS50");
        request.setFormaPagamento("PIX");

        assertThrows(ErroCheckout.class, () -> checkoutService.calcularResumo(request));
    }

    @Test
    public void testeErro_FormaPagamentoInvalida() {
        CheckoutRequestDTO request = new CheckoutRequestDTO();
        request.setItens(Collections.singletonList(
            new ItemPedidoDTO("Produto", 100.0, 1, 1.0)
        ));
        request.setModalidadeEntrega("ECONOMICA");
        request.setFormaPagamento("INVALIDA");

        assertThrows(ErroCheckout.class, () -> checkoutService.calcularResumo(request));
    }

    @Test
    public void testeErro_ParcelamentoInvalido_PixComMaisDe1Parcela() {
        CheckoutRequestDTO request = new CheckoutRequestDTO();
        request.setItens(Collections.singletonList(
            new ItemPedidoDTO("Produto", 100.0, 1, 1.0)
        ));
        request.setModalidadeEntrega("ECONOMICA");
        request.setFormaPagamento("PIX");
        request.setParcelas(2);

        assertThrows(ErroCheckout.class, () -> checkoutService.calcularResumo(request));
    }

    @Test
    public void testeErro_FormaPagamentoIndisponivel_BoletoAcima1000() {
        CheckoutRequestDTO request = new CheckoutRequestDTO();
        request.setItens(Collections.singletonList(
            new ItemPedidoDTO("Produto", 600.0, 2, 1.0)
        ));
        request.setModalidadeEntrega("ECONOMICA");
        request.setFormaPagamento("BOLETO");
        request.setParcelas(1);

        assertThrows(ErroCheckout.class, () -> checkoutService.calcularResumo(request));
    }

    @Test
    public void testeCupomFreteGratis() {
        CheckoutRequestDTO request = new CheckoutRequestDTO();
        request.setItens(Collections.singletonList(
            new ItemPedidoDTO("Produto", 100.0, 1, 2.0)
        ));
        request.setModalidadeEntrega("EXPRESSA");
        request.setCupom("FRETEGRATIS");
        request.setFormaPagamento("PIX");

        CheckoutResponseDTO response = checkoutService.calcularResumo(request);

        assertEquals(new BigDecimal("100.00"), response.getSubtotalProdutos());
        BigDecimal freteEsperado = new BigDecimal("25.00").add(new BigDecimal("4.50").multiply(new BigDecimal("2.0")));
        freteEsperado = freteEsperado.setScale(2, java.math.RoundingMode.HALF_EVEN);
        assertEquals(freteEsperado, response.getDescontoCupom());
        assertEquals(new BigDecimal("34.00"), response.getFrete());
    }

    @Test
    public void testeParcelasCartaoSemJuros() {
        CheckoutRequestDTO request = new CheckoutRequestDTO();
        request.setItens(Collections.singletonList(
            new ItemPedidoDTO("Produto", 300.0, 1, 1.0)
        ));
        request.setModalidadeEntrega("ECONOMICA");
        request.setFormaPagamento("CARTAO");
        request.setParcelas(3);

        CheckoutResponseDTO response = checkoutService.calcularResumo(request);

        assertEquals(BigDecimal.ZERO.setScale(2, java.math.RoundingMode.HALF_EVEN), response.getAjustePagamento());
        assertEquals(new BigDecimal("314.00"), response.getTotalFinal());
        assertEquals(new BigDecimal("104.67"), response.getValorParcela());
    }

    @Test
    public void testeParcelasCartaoComJuros() {
        CheckoutRequestDTO request = new CheckoutRequestDTO();
        request.setItens(Collections.singletonList(
            new ItemPedidoDTO("Produto", 300.0, 1, 1.0)
        ));
        request.setModalidadeEntrega("ECONOMICA");
        request.setFormaPagamento("CARTAO");
        request.setParcelas(6);

        CheckoutResponseDTO response = checkoutService.calcularResumo(request);

        assertNotEquals(new BigDecimal("0.00"), response.getAjustePagamento());
        assertTrue(response.getAjustePagamento().compareTo(BigDecimal.ZERO) > 0);
    }

}
