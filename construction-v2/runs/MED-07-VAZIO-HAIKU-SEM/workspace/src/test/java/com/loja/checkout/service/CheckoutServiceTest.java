package com.loja.checkout.service;

import com.loja.checkout.error.CheckoutException;
import com.loja.checkout.model.CheckoutRequest;
import com.loja.checkout.model.CheckoutResponse;
import com.loja.checkout.model.Item;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

public class CheckoutServiceTest {

    private CheckoutService service;

    @BeforeEach
    public void setup() {
        service = new CheckoutService();
    }

    @Test
    public void testExemplo1() {
        // Camiseta 79,90 × 2 (0,30 kg) + Tênis 249,90 × 1 (1,20 kg), EXPRESSA, cupom BEMVINDO10, PIX
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Arrays.asList(
                new Item("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
                new Item("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"))
        ));
        request.setModalidadeEntrega("EXPRESSA");
        request.setCupom("BEMVINDO10");
        request.setFormaPagamento("PIX");
        request.setParcelas(1);

        CheckoutResponse response = service.calcularResumo(request);

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
    public void testExemplo2() {
        // mesmos itens, ECONOMICA, sem cupom, CARTAO em 6×
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Arrays.asList(
                new Item("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
                new Item("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"))
        ));
        request.setModalidadeEntrega("ECONOMICA");
        request.setCupom(null);
        request.setFormaPagamento("CARTAO");
        request.setParcelas(6);

        CheckoutResponse response = service.calcularResumo(request);

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
    public void testExemplo3() {
        // Fone 199,90 × 2 (0,25 kg), MOTOBOY, cupom MENOS50, BOLETO
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Arrays.asList(
                new Item("Fone", new BigDecimal("199.90"), 2, new BigDecimal("0.25"))
        ));
        request.setModalidadeEntrega("MOTOBOY");
        request.setCupom("MENOS50");
        request.setFormaPagamento("BOLETO");
        request.setParcelas(1);

        CheckoutResponse response = service.calcularResumo(request);

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
    public void testExemplo4() {
        // Meia 19,90 × 7 (0,10 kg) + Camiseta 79,90 × 2 (0,30 kg), RETIRADA_LOJA, cupom LEVE3PAGUE2, CARTAO em 3×
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Arrays.asList(
                new Item("Meia", new BigDecimal("19.90"), 7, new BigDecimal("0.10")),
                new Item("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30"))
        ));
        request.setModalidadeEntrega("RETIRADA_LOJA");
        request.setCupom("LEVE3PAGUE2");
        request.setFormaPagamento("CARTAO");
        request.setParcelas(3);

        CheckoutResponse response = service.calcularResumo(request);

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
    public void testCarrinhoVazio() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Arrays.asList());
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("PIX");

        CheckoutException exception = assertThrows(CheckoutException.class, () -> {
            service.calcularResumo(request);
        });

        assertEquals("PEDIDO_INVALIDO", exception.getCodigo());
    }

    @Test
    public void testPrecoInvalido() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Arrays.asList(
                new Item("Produto", new BigDecimal("0"), 1, new BigDecimal("0.5"))
        ));
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("PIX");

        CheckoutException exception = assertThrows(CheckoutException.class, () -> {
            service.calcularResumo(request);
        });

        assertEquals("PEDIDO_INVALIDO", exception.getCodigo());
    }

    @Test
    public void testModalidadeInvalida() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Arrays.asList(
                new Item("Produto", new BigDecimal("50.00"), 1, new BigDecimal("0.5"))
        ));
        request.setModalidadeEntrega("INVALIDA");
        request.setFormaPagamento("PIX");

        CheckoutException exception = assertThrows(CheckoutException.class, () -> {
            service.calcularResumo(request);
        });

        assertEquals("MODALIDADE_INVALIDA", exception.getCodigo());
    }

    @Test
    public void testMotoboySemModalidade() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Arrays.asList(
                new Item("Produto", new BigDecimal("50.00"), 1, new BigDecimal("0.5"))
        ));
        request.setModalidadeEntrega(null);
        request.setFormaPagamento("PIX");

        CheckoutException exception = assertThrows(CheckoutException.class, () -> {
            service.calcularResumo(request);
        });

        assertEquals("MODALIDADE_INVALIDA", exception.getCodigo());
    }

    @Test
    public void testMotoboySobreoPeso() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Arrays.asList(
                new Item("Produto", new BigDecimal("50.00"), 1, new BigDecimal("6.0"))
        ));
        request.setModalidadeEntrega("MOTOBOY");
        request.setFormaPagamento("PIX");

        CheckoutException exception = assertThrows(CheckoutException.class, () -> {
            service.calcularResumo(request);
        });

        assertEquals("MODALIDADE_INDISPONIVEL", exception.getCodigo());
    }

    @Test
    public void testCupomInvalido() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Arrays.asList(
                new Item("Produto", new BigDecimal("50.00"), 1, new BigDecimal("0.5"))
        ));
        request.setModalidadeEntrega("EXPRESSA");
        request.setCupom("INVALIDO");
        request.setFormaPagamento("PIX");

        CheckoutException exception = assertThrows(CheckoutException.class, () -> {
            service.calcularResumo(request);
        });

        assertEquals("CUPOM_INVALIDO", exception.getCodigo());
    }

    @Test
    public void testMenos50Abaixo300() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Arrays.asList(
                new Item("Produto", new BigDecimal("100.00"), 1, new BigDecimal("0.5"))
        ));
        request.setModalidadeEntrega("EXPRESSA");
        request.setCupom("MENOS50");
        request.setFormaPagamento("PIX");

        CheckoutException exception = assertThrows(CheckoutException.class, () -> {
            service.calcularResumo(request);
        });

        assertEquals("CUPOM_NAO_APLICAVEL", exception.getCodigo());
    }

    @Test
    public void testFormaPagamentoInvalida() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Arrays.asList(
                new Item("Produto", new BigDecimal("50.00"), 1, new BigDecimal("0.5"))
        ));
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("INVALIDA");

        CheckoutException exception = assertThrows(CheckoutException.class, () -> {
            service.calcularResumo(request);
        });

        assertEquals("FORMA_PAGAMENTO_INVALIDA", exception.getCodigo());
    }

    @Test
    public void testPixParcelado() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Arrays.asList(
                new Item("Produto", new BigDecimal("100.00"), 1, new BigDecimal("0.5"))
        ));
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("PIX");
        request.setParcelas(2);

        CheckoutException exception = assertThrows(CheckoutException.class, () -> {
            service.calcularResumo(request);
        });

        assertEquals("PARCELAMENTO_INVALIDO", exception.getCodigo());
    }

    @Test
    public void testBoletoParcelado() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Arrays.asList(
                new Item("Produto", new BigDecimal("100.00"), 1, new BigDecimal("0.5"))
        ));
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("BOLETO");
        request.setParcelas(2);

        CheckoutException exception = assertThrows(CheckoutException.class, () -> {
            service.calcularResumo(request);
        });

        assertEquals("PARCELAMENTO_INVALIDO", exception.getCodigo());
    }

    @Test
    public void testCartaoParcelasAcima12() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Arrays.asList(
                new Item("Produto", new BigDecimal("1000.00"), 1, new BigDecimal("0.5"))
        ));
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("CARTAO");
        request.setParcelas(13);

        CheckoutException exception = assertThrows(CheckoutException.class, () -> {
            service.calcularResumo(request);
        });

        assertEquals("PARCELAMENTO_INVALIDO", exception.getCodigo());
    }

    @Test
    public void testFretegratis() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Arrays.asList(
                new Item("Produto", new BigDecimal("100.00"), 1, new BigDecimal("0.5"))
        ));
        request.setModalidadeEntrega("EXPRESSA");
        request.setCupom("FRETEGRATIS");
        request.setFormaPagamento("PIX");

        CheckoutResponse response = service.calcularResumo(request);

        assertEquals(new BigDecimal("100.00"), response.getSubtotalProdutos());
        assertEquals(response.getFrete(), response.getDescontoCupom());
        assertTrue(response.getTotalFinal().compareTo(new BigDecimal("100.00")) < 0);
    }

    @Test
    public void testSemParcelas() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Arrays.asList(
                new Item("Produto", new BigDecimal("100.00"), 1, new BigDecimal("0.5"))
        ));
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("PIX");
        request.setParcelas(null);

        CheckoutResponse response = service.calcularResumo(request);

        assertEquals(1, response.getParcelas());
    }
}
