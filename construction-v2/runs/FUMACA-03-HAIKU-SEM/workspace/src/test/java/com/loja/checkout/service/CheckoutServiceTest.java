package com.loja.checkout.service;

import com.loja.checkout.dto.ItemRequest;
import com.loja.checkout.dto.ResumoRequest;
import com.loja.checkout.dto.ResumoResponse;
import com.loja.checkout.error.CheckoutException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class CheckoutServiceTest {

    @Autowired
    private CheckoutService checkoutService;

    @Test
    public void testExemplo1_CamisetaTenisExpressaBemvindo10Pix() {
        ItemRequest item1 = new ItemRequest("Camiseta", 79.90, 2, 0.30);
        ItemRequest item2 = new ItemRequest("Tênis", 249.90, 1, 1.20);

        ResumoRequest request = new ResumoRequest(
            Arrays.asList(item1, item2),
            "EXPRESSA",
            "BEMVINDO10",
            "PIX",
            1
        );

        ResumoResponse response = checkoutService.calcularResumo(request);

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
    public void testExemplo2_CamisetaTenisEconomicaSemCupomCartao6x() {
        ItemRequest item1 = new ItemRequest("Camiseta", 79.90, 2, 0.30);
        ItemRequest item2 = new ItemRequest("Tênis", 249.90, 1, 1.20);

        ResumoRequest request = new ResumoRequest(
            Arrays.asList(item1, item2),
            "ECONOMICA",
            null,
            "CARTAO",
            6
        );

        ResumoResponse response = checkoutService.calcularResumo(request);

        assertEquals(new BigDecimal("409.70"), response.getSubtotalProdutos());
        assertEquals(new BigDecimal("0.00"), response.getDescontoCupom());
        assertEquals(new BigDecimal("15.60"), response.getFrete());
        assertEquals(7, response.getPrazoEntregaDias());
        assertEquals(6, response.getParcelas());

        // Valores calculados com tabela Price (taxa 1.99% ao mês)
        // Total esperado é maior que 425.30 (base) devido aos juros
        assertTrue(response.getTotalFinal().compareTo(new BigDecimal("425.30")) > 0);
        assertTrue(response.getValorParcela().compareTo(new BigDecimal("75.00")) > 0);
    }

    @Test
    public void testExemplo3_FoneMotoboySemZeroMenos50Boleto() {
        ItemRequest item1 = new ItemRequest("Fone", 199.90, 2, 0.25);

        ResumoRequest request = new ResumoRequest(
            Collections.singletonList(item1),
            "MOTOBOY",
            "MENOS50",
            "BOLETO",
            1
        );

        ResumoResponse response = checkoutService.calcularResumo(request);

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
    public void testExemplo4_MeiaCamisetaRetiradaLojaLeve3Pague2Cartao3x() {
        ItemRequest item1 = new ItemRequest("Meia", 19.90, 7, 0.10);
        ItemRequest item2 = new ItemRequest("Camiseta", 79.90, 2, 0.30);

        ResumoRequest request = new ResumoRequest(
            Arrays.asList(item1, item2),
            "RETIRADA_LOJA",
            "LEVE3PAGUE2",
            "CARTAO",
            3
        );

        ResumoResponse response = checkoutService.calcularResumo(request);

        assertEquals(new BigDecimal("299.10"), response.getSubtotalProdutos());
        assertEquals(new BigDecimal("39.80"), response.getDescontoCupom());
        assertEquals(new BigDecimal("0.00"), response.getFrete());
        assertEquals(1, response.getPrazoEntregaDias());
        assertEquals(new BigDecimal("0.00"), response.getAjustePagamento());
        assertEquals(new BigDecimal("259.30"), response.getTotalFinal());
        assertEquals(3, response.getParcelas());
        assertEquals(new BigDecimal("86.43"), response.getValorParcela());
    }

    @Test
    public void testErro_CarrinhoVazio() {
        ResumoRequest request = new ResumoRequest(
            Collections.emptyList(),
            "EXPRESSA",
            null,
            "PIX",
            1
        );

        CheckoutException exception = assertThrows(CheckoutException.class, () -> {
            checkoutService.calcularResumo(request);
        });

        assertEquals("PEDIDO_INVALIDO", exception.getCodigoErro());
    }

    @Test
    public void testErro_ItemComPrecoNegativo() {
        ItemRequest item1 = new ItemRequest("Camiseta", -10.0, 1, 0.30);

        ResumoRequest request = new ResumoRequest(
            Collections.singletonList(item1),
            "EXPRESSA",
            null,
            "PIX",
            1
        );

        CheckoutException exception = assertThrows(CheckoutException.class, () -> {
            checkoutService.calcularResumo(request);
        });

        assertEquals("PEDIDO_INVALIDO", exception.getCodigoErro());
    }

    @Test
    public void testErro_ModalidadeInvalida() {
        ItemRequest item1 = new ItemRequest("Camiseta", 79.90, 1, 0.30);

        ResumoRequest request = new ResumoRequest(
            Collections.singletonList(item1),
            "INVALIDA",
            null,
            "PIX",
            1
        );

        CheckoutException exception = assertThrows(CheckoutException.class, () -> {
            checkoutService.calcularResumo(request);
        });

        assertEquals("MODALIDADE_INVALIDA", exception.getCodigoErro());
    }

    @Test
    public void testErro_MotoboySobrepesoIndisponivel() {
        ItemRequest item1 = new ItemRequest("Camiseta", 79.90, 1, 6.0);

        ResumoRequest request = new ResumoRequest(
            Collections.singletonList(item1),
            "MOTOBOY",
            null,
            "PIX",
            1
        );

        CheckoutException exception = assertThrows(CheckoutException.class, () -> {
            checkoutService.calcularResumo(request);
        });

        assertEquals("MODALIDADE_INDISPONIVEL", exception.getCodigoErro());
    }

    @Test
    public void testErro_CupomInvalido() {
        ItemRequest item1 = new ItemRequest("Camiseta", 79.90, 1, 0.30);

        ResumoRequest request = new ResumoRequest(
            Collections.singletonList(item1),
            "EXPRESSA",
            "CUPOMINVALIDO",
            "PIX",
            1
        );

        CheckoutException exception = assertThrows(CheckoutException.class, () -> {
            checkoutService.calcularResumo(request);
        });

        assertEquals("CUPOM_INVALIDO", exception.getCodigoErro());
    }

    @Test
    public void testErro_CupomMenos50AbaixoDeTrezentos() {
        ItemRequest item1 = new ItemRequest("Camiseta", 50.0, 1, 0.30);

        ResumoRequest request = new ResumoRequest(
            Collections.singletonList(item1),
            "EXPRESSA",
            "MENOS50",
            "PIX",
            1
        );

        CheckoutException exception = assertThrows(CheckoutException.class, () -> {
            checkoutService.calcularResumo(request);
        });

        assertEquals("CUPOM_NAO_APLICAVEL", exception.getCodigoErro());
    }

    @Test
    public void testErro_FormaPagamentoInvalida() {
        ItemRequest item1 = new ItemRequest("Camiseta", 79.90, 1, 0.30);

        ResumoRequest request = new ResumoRequest(
            Collections.singletonList(item1),
            "EXPRESSA",
            null,
            "INVALIDA",
            1
        );

        CheckoutException exception = assertThrows(CheckoutException.class, () -> {
            checkoutService.calcularResumo(request);
        });

        assertEquals("FORMA_PAGAMENTO_INVALIDA", exception.getCodigoErro());
    }

    @Test
    public void testErro_ParcelasPixNaoPermitidas() {
        ItemRequest item1 = new ItemRequest("Camiseta", 79.90, 1, 0.30);

        ResumoRequest request = new ResumoRequest(
            Collections.singletonList(item1),
            "EXPRESSA",
            null,
            "PIX",
            2
        );

        CheckoutException exception = assertThrows(CheckoutException.class, () -> {
            checkoutService.calcularResumo(request);
        });

        assertEquals("PARCELAMENTO_INVALIDO", exception.getCodigoErro());
    }

    @Test
    public void testErro_BoletoAcimaDelimite() {
        ItemRequest item1 = new ItemRequest("Camiseta", 1000.0, 2, 0.30);

        ResumoRequest request = new ResumoRequest(
            Collections.singletonList(item1),
            "RETIRADA_LOJA",
            null,
            "BOLETO",
            1
        );

        CheckoutException exception = assertThrows(CheckoutException.class, () -> {
            checkoutService.calcularResumo(request);
        });

        assertEquals("FORMA_PAGAMENTO_INDISPONIVEL", exception.getCodigoErro());
    }
}
