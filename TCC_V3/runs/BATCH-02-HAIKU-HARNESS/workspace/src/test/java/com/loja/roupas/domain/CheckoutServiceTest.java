package com.loja.roupas.domain;

import com.loja.roupas.dto.ItemCarrinho;
import com.loja.roupas.dto.ResumoCheckoutRequest;
import com.loja.roupas.dto.ResumoCheckoutResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CheckoutServiceTest {
    private CheckoutService service;

    @BeforeEach
    void setUp() {
        service = new CheckoutService();
    }

    @Test
    void exemplo1_CamisetaTenisExpressaBemVindoPix() throws CheckoutException {
        List<ItemCarrinho> itens = Arrays.asList(
            new ItemCarrinho("Camiseta", 79.90, 2, 0.30),
            new ItemCarrinho("Tênis", 249.90, 1, 1.20)
        );

        ResumoCheckoutRequest request = new ResumoCheckoutRequest(
            itens, "EXPRESSA", "BEMVINDO10", "PIX", 1
        );

        ResumoCheckoutResponse response = service.calcular(request);

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
    void exemplo2_CamisetaTenisEconomicaCartao6x() throws CheckoutException {
        List<ItemCarrinho> itens = Arrays.asList(
            new ItemCarrinho("Camiseta", 79.90, 2, 0.30),
            new ItemCarrinho("Tênis", 249.90, 1, 1.20)
        );

        ResumoCheckoutRequest request = new ResumoCheckoutRequest(
            itens, "ECONOMICA", null, "CARTAO", 6
        );

        ResumoCheckoutResponse response = service.calcular(request);

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
    void exemplo3_FoneMotoboySemJurosBoleto() throws CheckoutException {
        List<ItemCarrinho> itens = Arrays.asList(
            new ItemCarrinho("Fone", 199.90, 2, 0.25)
        );

        ResumoCheckoutRequest request = new ResumoCheckoutRequest(
            itens, "MOTOBOY", "MENOS50", "BOLETO", null
        );

        ResumoCheckoutResponse response = service.calcular(request);

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
    void exemplo4_MeiaLeve3Pague2Cartao3x() throws CheckoutException {
        List<ItemCarrinho> itens = Arrays.asList(
            new ItemCarrinho("Meia", 19.90, 7, 0.10),
            new ItemCarrinho("Camiseta", 79.90, 2, 0.30)
        );

        ResumoCheckoutRequest request = new ResumoCheckoutRequest(
            itens, "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3
        );

        ResumoCheckoutResponse response = service.calcular(request);

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
    void erroCarrinhoVazio() {
        List<ItemCarrinho> itens = Arrays.asList();

        ResumoCheckoutRequest request = new ResumoCheckoutRequest(
            itens, "EXPRESSA", null, "PIX", 1
        );

        CheckoutException exception = assertThrows(CheckoutException.class, () -> {
            service.calcular(request);
        });

        assertEquals("PEDIDO_INVALIDO", exception.getCodigo());
    }

    @Test
    void erroItemComPrecoZero() {
        List<ItemCarrinho> itens = Arrays.asList(
            new ItemCarrinho("Camiseta", 0.0, 1, 0.30)
        );

        ResumoCheckoutRequest request = new ResumoCheckoutRequest(
            itens, "EXPRESSA", null, "PIX", 1
        );

        CheckoutException exception = assertThrows(CheckoutException.class, () -> {
            service.calcular(request);
        });

        assertEquals("PEDIDO_INVALIDO", exception.getCodigo());
    }

    @Test
    void erroModalidadeInvalida() {
        List<ItemCarrinho> itens = Arrays.asList(
            new ItemCarrinho("Camiseta", 79.90, 1, 0.30)
        );

        ResumoCheckoutRequest request = new ResumoCheckoutRequest(
            itens, "INVALIDA", null, "PIX", 1
        );

        CheckoutException exception = assertThrows(CheckoutException.class, () -> {
            service.calcular(request);
        });

        assertEquals("MODALIDADE_INVALIDA", exception.getCodigo());
    }

    @Test
    void erroMotoboySobrepeso() {
        List<ItemCarrinho> itens = Arrays.asList(
            new ItemCarrinho("Caixa", 100.0, 1, 6.0)
        );

        ResumoCheckoutRequest request = new ResumoCheckoutRequest(
            itens, "MOTOBOY", null, "PIX", 1
        );

        CheckoutException exception = assertThrows(CheckoutException.class, () -> {
            service.calcular(request);
        });

        assertEquals("MODALIDADE_INDISPONIVEL", exception.getCodigo());
    }

    @Test
    void erroCupomInvalido() {
        List<ItemCarrinho> itens = Arrays.asList(
            new ItemCarrinho("Camiseta", 79.90, 1, 0.30)
        );

        ResumoCheckoutRequest request = new ResumoCheckoutRequest(
            itens, "EXPRESSA", "INVALIDO", "PIX", 1
        );

        CheckoutException exception = assertThrows(CheckoutException.class, () -> {
            service.calcular(request);
        });

        assertEquals("CUPOM_INVALIDO", exception.getCodigo());
    }

    @Test
    void erroCupomNaoAplicavelMenos50() {
        List<ItemCarrinho> itens = Arrays.asList(
            new ItemCarrinho("Camiseta", 79.90, 1, 0.30)
        );

        ResumoCheckoutRequest request = new ResumoCheckoutRequest(
            itens, "EXPRESSA", "MENOS50", "PIX", 1
        );

        CheckoutException exception = assertThrows(CheckoutException.class, () -> {
            service.calcular(request);
        });

        assertEquals("CUPOM_NAO_APLICAVEL", exception.getCodigo());
    }

    @Test
    void erroFormaPagamentoInvalida() {
        List<ItemCarrinho> itens = Arrays.asList(
            new ItemCarrinho("Camiseta", 79.90, 1, 0.30)
        );

        ResumoCheckoutRequest request = new ResumoCheckoutRequest(
            itens, "EXPRESSA", null, "INVALIDA", 1
        );

        CheckoutException exception = assertThrows(CheckoutException.class, () -> {
            service.calcular(request);
        });

        assertEquals("FORMA_PAGAMENTO_INVALIDA", exception.getCodigo());
    }

    @Test
    void erroParcelamentoInvalidoPixAcima1() {
        List<ItemCarrinho> itens = Arrays.asList(
            new ItemCarrinho("Camiseta", 79.90, 1, 0.30)
        );

        ResumoCheckoutRequest request = new ResumoCheckoutRequest(
            itens, "EXPRESSA", null, "PIX", 2
        );

        CheckoutException exception = assertThrows(CheckoutException.class, () -> {
            service.calcular(request);
        });

        assertEquals("PARCELAMENTO_INVALIDO", exception.getCodigo());
    }

    @Test
    void erroParcelamentoInvalidoBoletoAcima1() {
        List<ItemCarrinho> itens = Arrays.asList(
            new ItemCarrinho("Camiseta", 79.90, 1, 0.30)
        );

        ResumoCheckoutRequest request = new ResumoCheckoutRequest(
            itens, "EXPRESSA", null, "BOLETO", 2
        );

        CheckoutException exception = assertThrows(CheckoutException.class, () -> {
            service.calcular(request);
        });

        assertEquals("PARCELAMENTO_INVALIDO", exception.getCodigo());
    }

    @Test
    void erroParcelamentoInvalidoCartaoAcima12() {
        List<ItemCarrinho> itens = Arrays.asList(
            new ItemCarrinho("Camiseta", 79.90, 1, 0.30)
        );

        ResumoCheckoutRequest request = new ResumoCheckoutRequest(
            itens, "EXPRESSA", null, "CARTAO", 13
        );

        CheckoutException exception = assertThrows(CheckoutException.class, () -> {
            service.calcular(request);
        });

        assertEquals("PARCELAMENTO_INVALIDO", exception.getCodigo());
    }

    @Test
    void erroFormaPagamentoIndisponiavelBoletoAcimaLimite() {
        List<ItemCarrinho> itens = Arrays.asList(
            new ItemCarrinho("Produto caro", 1001.0, 1, 0.30)
        );

        ResumoCheckoutRequest request = new ResumoCheckoutRequest(
            itens, "RETIRADA_LOJA", null, "BOLETO", 1
        );

        CheckoutException exception = assertThrows(CheckoutException.class, () -> {
            service.calcular(request);
        });

        assertEquals("FORMA_PAGAMENTO_INDISPONIVEL", exception.getCodigo());
    }

}
