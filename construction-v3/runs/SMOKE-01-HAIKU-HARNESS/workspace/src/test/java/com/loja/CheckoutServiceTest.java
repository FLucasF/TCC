package com.loja;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.loja.cupom.CupomRegistry;
import com.loja.model.CheckoutRequest;
import com.loja.model.CheckoutResponse;
import com.loja.model.Item;
import com.loja.payment.PaymentMethodRegistry;
import com.loja.shipping.ShippingMethodRegistry;

import static org.junit.jupiter.api.Assertions.*;

class CheckoutServiceTest {
    private CheckoutService checkoutService;

    @BeforeEach
    void setUp() {
        ShippingMethodRegistry shippingRegistry = new ShippingMethodRegistry();
        CupomRegistry cupomRegistry = new CupomRegistry();
        PaymentMethodRegistry paymentRegistry = new PaymentMethodRegistry();
        checkoutService = new CheckoutService(shippingRegistry, cupomRegistry, paymentRegistry);
    }

    @Test
    void testExample1() throws CheckoutException {
        // Camiseta 79,90 × 2 (0,30 kg) + Tênis 249,90 × 1 (1,20 kg)
        // EXPRESSA, cupom BEMVINDO10, PIX
        // Esperado: subtotal 409,70 · cupom 40,97 · frete 33,10 · prazo 2 · ajuste −20,09 · total final 381,74 · 1× de 381,74

        CheckoutRequest request = new CheckoutRequest();

        List<Item> itens = new ArrayList<>();
        itens.add(new Item("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")));
        itens.add(new Item("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20")));

        request.setItens(itens);
        request.setModalidadeEntrega("EXPRESSA");
        request.setCupom("BEMVINDO10");
        request.setFormaPagamento("PIX");
        request.setParcelas(1);

        CheckoutResponse response = checkoutService.calculate(request);

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
    void testExample2() throws CheckoutException {
        // mesmos itens, ECONOMICA, sem cupom, CARTAO em 6×
        // Esperado: subtotal 409,70 · cupom 0,00 · frete 15,60 · prazo 7 · ajuste 30,10 · total final 455,40 · 6× de 75,90

        CheckoutRequest request = new CheckoutRequest();

        List<Item> itens = new ArrayList<>();
        itens.add(new Item("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")));
        itens.add(new Item("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20")));

        request.setItens(itens);
        request.setModalidadeEntrega("ECONOMICA");
        request.setCupom(null);
        request.setFormaPagamento("CARTAO");
        request.setParcelas(6);

        CheckoutResponse response = checkoutService.calculate(request);

        assertEquals(new BigDecimal("409.70"), response.getSubtotalProdutos());
        assertEquals(new BigDecimal("0.00"), response.getDescontoCupom());
        assertEquals(new BigDecimal("15.60"), response.getFrete());
        assertEquals(7, response.getPrazoEntregaDias());
        assertTrue(response.getTotalFinal().compareTo(new BigDecimal("455.39")) >= 0 &&
                   response.getTotalFinal().compareTo(new BigDecimal("455.41")) <= 0);
        assertEquals(6, response.getParcelas());
        assertTrue(response.getValorParcela().compareTo(new BigDecimal("75.89")) >= 0 &&
                   response.getValorParcela().compareTo(new BigDecimal("75.91")) <= 0);
    }

    @Test
    void testExample3() throws CheckoutException {
        // Fone 199,90 × 2 (0,25 kg)
        // MOTOBOY, cupom MENOS50, BOLETO
        // Esperado: subtotal 399,80 · cupom 50,00 · frete 18,00 · prazo 0 · ajuste 3,49 · total final 371,29 · 1× de 371,29

        CheckoutRequest request = new CheckoutRequest();

        List<Item> itens = new ArrayList<>();
        itens.add(new Item("Fone", new BigDecimal("199.90"), 2, new BigDecimal("0.25")));

        request.setItens(itens);
        request.setModalidadeEntrega("MOTOBOY");
        request.setCupom("MENOS50");
        request.setFormaPagamento("BOLETO");
        request.setParcelas(1);

        CheckoutResponse response = checkoutService.calculate(request);

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
    void testExample4() throws CheckoutException {
        // Meia 19,90 × 7 (0,10 kg) + Camiseta 79,90 × 2 (0,30 kg)
        // RETIRADA_LOJA, cupom LEVE3PAGUE2, CARTAO em 3×
        // Esperado: subtotal 299,10 · cupom 39,80 · frete 0,00 · prazo 1 · ajuste 0,00 · total final 259,30 · 3× de 86,43

        CheckoutRequest request = new CheckoutRequest();

        List<Item> itens = new ArrayList<>();
        itens.add(new Item("Meia", new BigDecimal("19.90"), 7, new BigDecimal("0.10")));
        itens.add(new Item("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")));

        request.setItens(itens);
        request.setModalidadeEntrega("RETIRADA_LOJA");
        request.setCupom("LEVE3PAGUE2");
        request.setFormaPagamento("CARTAO");
        request.setParcelas(3);

        CheckoutResponse response = checkoutService.calculate(request);

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
    void testEmptyCart() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(new ArrayList<>());
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("PIX");

        assertThrows(CheckoutException.class, () -> checkoutService.calculate(request));
    }

    @Test
    void testInvalidDelivery() {
        CheckoutRequest request = new CheckoutRequest();

        List<Item> itens = new ArrayList<>();
        itens.add(new Item("Produto", new BigDecimal("100.00"), 1, new BigDecimal("0.50")));

        request.setItens(itens);
        request.setModalidadeEntrega("INVALIDA");
        request.setFormaPagamento("PIX");

        assertThrows(CheckoutException.class, () -> checkoutService.calculate(request));
    }

    @Test
    void testMotoboyUnavailable() {
        CheckoutRequest request = new CheckoutRequest();

        List<Item> itens = new ArrayList<>();
        itens.add(new Item("Produto", new BigDecimal("100.00"), 10, new BigDecimal("1.00")));

        request.setItens(itens);
        request.setModalidadeEntrega("MOTOBOY");
        request.setFormaPagamento("PIX");

        assertThrows(CheckoutException.class, () -> checkoutService.calculate(request));
    }

    @Test
    void testInvalidCupom() {
        CheckoutRequest request = new CheckoutRequest();

        List<Item> itens = new ArrayList<>();
        itens.add(new Item("Produto", new BigDecimal("100.00"), 1, new BigDecimal("0.50")));

        request.setItens(itens);
        request.setModalidadeEntrega("EXPRESSA");
        request.setCupom("INVALIDO");
        request.setFormaPagamento("PIX");

        assertThrows(CheckoutException.class, () -> checkoutService.calculate(request));
    }

    @Test
    void testMenos50NotApplicable() {
        CheckoutRequest request = new CheckoutRequest();

        List<Item> itens = new ArrayList<>();
        itens.add(new Item("Produto", new BigDecimal("100.00"), 1, new BigDecimal("0.50")));

        request.setItens(itens);
        request.setModalidadeEntrega("EXPRESSA");
        request.setCupom("MENOS50");
        request.setFormaPagamento("PIX");

        assertThrows(CheckoutException.class, () -> checkoutService.calculate(request));
    }

    @Test
    void testInvalidPaymentMethod() {
        CheckoutRequest request = new CheckoutRequest();

        List<Item> itens = new ArrayList<>();
        itens.add(new Item("Produto", new BigDecimal("100.00"), 1, new BigDecimal("0.50")));

        request.setItens(itens);
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("INVALIDO");

        assertThrows(CheckoutException.class, () -> checkoutService.calculate(request));
    }

    @Test
    void testPixMultipleInstallments() {
        CheckoutRequest request = new CheckoutRequest();

        List<Item> itens = new ArrayList<>();
        itens.add(new Item("Produto", new BigDecimal("100.00"), 1, new BigDecimal("0.50")));

        request.setItens(itens);
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("PIX");
        request.setParcelas(2);

        assertThrows(CheckoutException.class, () -> checkoutService.calculate(request));
    }

    @Test
    void testBoletoHighAmount() {
        CheckoutRequest request = new CheckoutRequest();

        List<Item> itens = new ArrayList<>();
        itens.add(new Item("Produto", new BigDecimal("999.00"), 2, new BigDecimal("1.00")));

        request.setItens(itens);
        request.setModalidadeEntrega("RETIRADA_LOJA");
        request.setFormaPagamento("BOLETO");
        request.setParcelas(1);

        assertThrows(CheckoutException.class, () -> checkoutService.calculate(request));
    }
}
