package com.loja.roupas.checkout;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class CheckoutResumoServiceTest {

    private CheckoutResumoService service;

    @BeforeEach
    void setUp() {
        service = new CheckoutResumoService();
    }

    @Test
    void exemplo1_CamisetaTenisExpressaBemvindo10Pix() {
        List<ItemCarrinho> itens = Arrays.asList(
            new ItemCarrinho("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
            new ItemCarrinho("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"))
        );

        CheckoutRequest request = new CheckoutRequest(
            itens,
            "EXPRESSA",
            "BEMVINDO10",
            "PIX",
            1
        );

        CheckoutResponse response = service.calcular(request);

        assertEquals(new BigDecimal("409.70"), response.subtotalProdutos());
        assertEquals(new BigDecimal("40.97"), response.descontoCupom());
        assertEquals(new BigDecimal("33.10"), response.frete());
        assertEquals(2, response.prazoEntregaDias());
        assertEquals(new BigDecimal("-20.09"), response.ajustePagamento());
        assertEquals(new BigDecimal("381.74"), response.totalFinal());
        assertEquals(1, response.parcelas());
        assertEquals(new BigDecimal("381.74"), response.valorParcela());
    }

    @Test
    void exemplo2_CamisetaTenisEconomicaSemCupomCartao6x() {
        List<ItemCarrinho> itens = Arrays.asList(
            new ItemCarrinho("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
            new ItemCarrinho("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"))
        );

        CheckoutRequest request = new CheckoutRequest(
            itens,
            "ECONOMICA",
            null,
            "CARTAO",
            6
        );

        CheckoutResponse response = service.calcular(request);

        assertEquals(new BigDecimal("409.70"), response.subtotalProdutos());
        assertEquals(new BigDecimal("0.00"), response.descontoCupom());
        assertEquals(new BigDecimal("15.60"), response.frete());
        assertEquals(7, response.prazoEntregaDias());
        assertEquals(new BigDecimal("29.62"), response.ajustePagamento());
        assertEquals(new BigDecimal("454.92"), response.totalFinal());
        assertEquals(6, response.parcelas());
        assertEquals(new BigDecimal("75.82"), response.valorParcela());
    }

    @Test
    void exemplo3_FoneMotoboyCupomMenos50Boleto() {
        List<ItemCarrinho> itens = Arrays.asList(
            new ItemCarrinho("Fone", new BigDecimal("199.90"), 2, new BigDecimal("0.25"))
        );

        CheckoutRequest request = new CheckoutRequest(
            itens,
            "MOTOBOY",
            "MENOS50",
            "BOLETO",
            1
        );

        CheckoutResponse response = service.calcular(request);

        assertEquals(new BigDecimal("399.80"), response.subtotalProdutos());
        assertEquals(new BigDecimal("50.00"), response.descontoCupom());
        assertEquals(new BigDecimal("18.00"), response.frete());
        assertEquals(0, response.prazoEntregaDias());
        assertEquals(new BigDecimal("3.49"), response.ajustePagamento());
        assertEquals(new BigDecimal("371.29"), response.totalFinal());
        assertEquals(1, response.parcelas());
        assertEquals(new BigDecimal("371.29"), response.valorParcela());
    }

    @Test
    void exemplo4_MeiaCamisetaRetiradaLojaCupomLeve3Pague2Cartao3x() {
        List<ItemCarrinho> itens = Arrays.asList(
            new ItemCarrinho("Meia", new BigDecimal("19.90"), 7, new BigDecimal("0.10")),
            new ItemCarrinho("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30"))
        );

        CheckoutRequest request = new CheckoutRequest(
            itens,
            "RETIRADA_LOJA",
            "LEVE3PAGUE2",
            "CARTAO",
            3
        );

        CheckoutResponse response = service.calcular(request);

        assertEquals(new BigDecimal("299.10"), response.subtotalProdutos());
        assertEquals(new BigDecimal("39.80"), response.descontoCupom());
        assertEquals(new BigDecimal("0.00"), response.frete());
        assertEquals(1, response.prazoEntregaDias());
        assertEquals(new BigDecimal("0.00"), response.ajustePagamento());
        assertEquals(new BigDecimal("259.30"), response.totalFinal());
        assertEquals(3, response.parcelas());
        assertEquals(new BigDecimal("86.43"), response.valorParcela());  // 259.30 / 3 = 86.43 (arredondado)
    }

    @Test
    void validacao_PedidoVazio() {
        CheckoutRequest request = new CheckoutRequest(
            List.of(),
            "EXPRESSA",
            null,
            "PIX",
            1
        );

        assertThrows(ErroCheckout.class, () -> service.calcular(request), "PEDIDO_INVALIDO");
    }

    @Test
    void validacao_PrecoZero() {
        List<ItemCarrinho> itens = Arrays.asList(
            new ItemCarrinho("Produto", new BigDecimal("0.00"), 1, new BigDecimal("0.50"))
        );

        CheckoutRequest request = new CheckoutRequest(
            itens,
            "EXPRESSA",
            null,
            "PIX",
            1
        );

        assertThrows(ErroCheckout.class, () -> service.calcular(request));
    }

    @Test
    void validacao_QuantidadeZero() {
        List<ItemCarrinho> itens = Arrays.asList(
            new ItemCarrinho("Produto", new BigDecimal("100.00"), 0, new BigDecimal("0.50"))
        );

        CheckoutRequest request = new CheckoutRequest(
            itens,
            "EXPRESSA",
            null,
            "PIX",
            1
        );

        assertThrows(ErroCheckout.class, () -> service.calcular(request));
    }

    @Test
    void validacao_ModalidadeInvalida() {
        List<ItemCarrinho> itens = Arrays.asList(
            new ItemCarrinho("Produto", new BigDecimal("100.00"), 1, new BigDecimal("0.50"))
        );

        CheckoutRequest request = new CheckoutRequest(
            itens,
            "INVALIDA",
            null,
            "PIX",
            1
        );

        assertThrows(ErroCheckout.class, () -> service.calcular(request));
    }

    @Test
    void validacao_ModalidadeIndisponivel_MotoboySuperaLimite() {
        List<ItemCarrinho> itens = Arrays.asList(
            new ItemCarrinho("Produto", new BigDecimal("100.00"), 1, new BigDecimal("6.00"))
        );

        CheckoutRequest request = new CheckoutRequest(
            itens,
            "MOTOBOY",
            null,
            "PIX",
            1
        );

        assertThrows(ErroCheckout.class, () -> service.calcular(request));
    }

    @Test
    void validacao_CupomInvalido() {
        List<ItemCarrinho> itens = Arrays.asList(
            new ItemCarrinho("Produto", new BigDecimal("100.00"), 1, new BigDecimal("0.50"))
        );

        CheckoutRequest request = new CheckoutRequest(
            itens,
            "EXPRESSA",
            "CUPOMFALSO",
            "PIX",
            1
        );

        assertThrows(ErroCheckout.class, () -> service.calcular(request));
    }

    @Test
    void validacao_CupomNaoAplicavel_Menos50AbaixoMinimo() {
        List<ItemCarrinho> itens = Arrays.asList(
            new ItemCarrinho("Produto", new BigDecimal("100.00"), 1, new BigDecimal("0.50"))
        );

        CheckoutRequest request = new CheckoutRequest(
            itens,
            "EXPRESSA",
            "MENOS50",
            "PIX",
            1
        );

        assertThrows(ErroCheckout.class, () -> service.calcular(request));
    }

    @Test
    void validacao_FormaPagamentoInvalida() {
        List<ItemCarrinho> itens = Arrays.asList(
            new ItemCarrinho("Produto", new BigDecimal("100.00"), 1, new BigDecimal("0.50"))
        );

        CheckoutRequest request = new CheckoutRequest(
            itens,
            "EXPRESSA",
            null,
            "INVALIDA",
            1
        );

        assertThrows(ErroCheckout.class, () -> service.calcular(request));
    }

    @Test
    void validacao_ParcelamentoInvalido_PixComParcelas() {
        List<ItemCarrinho> itens = Arrays.asList(
            new ItemCarrinho("Produto", new BigDecimal("100.00"), 1, new BigDecimal("0.50"))
        );

        CheckoutRequest request = new CheckoutRequest(
            itens,
            "EXPRESSA",
            null,
            "PIX",
            2
        );

        assertThrows(ErroCheckout.class, () -> service.calcular(request));
    }

    @Test
    void validacao_FormaPagamentoIndisponivel_BoletoAcima1000() {
        List<ItemCarrinho> itens = Arrays.asList(
            new ItemCarrinho("Produto", new BigDecimal("1500.00"), 1, new BigDecimal("1.00"))
        );

        CheckoutRequest request = new CheckoutRequest(
            itens,
            "RETIRADA_LOJA",
            null,
            "BOLETO",
            1
        );

        assertThrows(ErroCheckout.class, () -> service.calcular(request));
    }

    @Test
    void cupomFretegratis() {
        List<ItemCarrinho> itens = Arrays.asList(
            new ItemCarrinho("Produto", new BigDecimal("100.00"), 1, new BigDecimal("1.00"))
        );

        CheckoutRequest request = new CheckoutRequest(
            itens,
            "EXPRESSA",
            "FRETEGRATIS",
            "PIX",
            1
        );

        CheckoutResponse response = service.calcular(request);

        assertEquals(new BigDecimal("100.00"), response.subtotalProdutos());
        assertEquals(new BigDecimal("29.50"), response.frete());
        assertEquals(new BigDecimal("29.50"), response.descontoCupom());
        assertTrue(response.totalFinal().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    void cartaoParcelas12ComJuros() {
        List<ItemCarrinho> itens = Arrays.asList(
            new ItemCarrinho("Produto", new BigDecimal("600.00"), 1, new BigDecimal("0.50"))
        );

        CheckoutRequest request = new CheckoutRequest(
            itens,
            "RETIRADA_LOJA",
            null,
            "CARTAO",
            12
        );

        CheckoutResponse response = service.calcular(request);

        assertEquals(12, response.parcelas());
        assertTrue(response.ajustePagamento().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(response.totalFinal().compareTo(new BigDecimal("600.00")) > 0);
    }
}
