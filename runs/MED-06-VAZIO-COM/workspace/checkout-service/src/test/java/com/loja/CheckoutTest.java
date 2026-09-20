package com.loja;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import com.loja.model.Item;
import com.loja.model.CheckoutRequest;
import com.loja.model.CheckoutResponse;
import com.loja.service.CheckoutService;

@SpringBootTest
public class CheckoutTest {

    @Autowired
    private CheckoutService checkoutService;

    @Test
    public void testExample1() throws Exception {
        // Camiseta 79,90 × 2 (0,30 kg) + Tênis 249,90 × 1 (1,20 kg), EXPRESSA, BEMVINDO10, PIX
        List<Item> items = Arrays.asList(
            new Item("Camiseta", 79.90, 2, 0.30),
            new Item("Tênis", 249.90, 1, 1.20)
        );

        CheckoutRequest request = new CheckoutRequest();
        request.setItens(items);
        request.setModalidadeEntrega("EXPRESSA");
        request.setCupom("BEMVINDO10");
        request.setFormaPagamento("PIX");

        CheckoutResponse response = checkoutService.calculateCheckout(request);

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
    public void testExample2() throws Exception {
        // Mesmos itens, ECONOMICA, sem cupom, CARTAO 6x
        List<Item> items = Arrays.asList(
            new Item("Camiseta", 79.90, 2, 0.30),
            new Item("Tênis", 249.90, 1, 1.20)
        );

        CheckoutRequest request = new CheckoutRequest();
        request.setItens(items);
        request.setModalidadeEntrega("ECONOMICA");
        request.setCupom(null);
        request.setFormaPagamento("CARTAO");
        request.setParcelas(6);

        CheckoutResponse response = checkoutService.calculateCheckout(request);

        assertEquals(new BigDecimal("409.70"), response.getSubtotalProdutos());
        assertEquals(0, response.getDescontoCupom().compareTo(new BigDecimal("0.00")));
        assertEquals(new BigDecimal("15.60"), response.getFrete());
        assertEquals(7, response.getPrazoEntregaDias());
        assertEquals(new BigDecimal("30.10"), response.getAjustePagamento());
        assertEquals(new BigDecimal("455.40"), response.getTotalFinal());
        assertEquals(6, response.getParcelas());
        assertEquals(new BigDecimal("75.90"), response.getValorParcela());
    }

    @Test
    public void testExample3() throws Exception {
        // Fone 199,90 × 2 (0,25 kg), MOTOBOY, MENOS50, BOLETO
        List<Item> items = Arrays.asList(
            new Item("Fone", 199.90, 2, 0.25)
        );

        CheckoutRequest request = new CheckoutRequest();
        request.setItens(items);
        request.setModalidadeEntrega("MOTOBOY");
        request.setCupom("MENOS50");
        request.setFormaPagamento("BOLETO");

        CheckoutResponse response = checkoutService.calculateCheckout(request);

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
    public void testExample4() throws Exception {
        // Meia 19,90 × 7 (0,10 kg) + Camiseta 79,90 × 2 (0,30 kg), RETIRADA_LOJA, LEVE3PAGUE2, CARTAO 3x
        List<Item> items = Arrays.asList(
            new Item("Meia", 19.90, 7, 0.10),
            new Item("Camiseta", 79.90, 2, 0.30)
        );

        CheckoutRequest request = new CheckoutRequest();
        request.setItens(items);
        request.setModalidadeEntrega("RETIRADA_LOJA");
        request.setCupom("LEVE3PAGUE2");
        request.setFormaPagamento("CARTAO");
        request.setParcelas(3);

        CheckoutResponse response = checkoutService.calculateCheckout(request);

        assertEquals(new BigDecimal("299.10"), response.getSubtotalProdutos());
        assertEquals(new BigDecimal("39.80"), response.getDescontoCupom());
        assertEquals(0, response.getFrete().compareTo(new BigDecimal("0.00")));
        assertEquals(1, response.getPrazoEntregaDias());
        assertEquals(0, response.getAjustePagamento().compareTo(new BigDecimal("0.00")));
        assertEquals(new BigDecimal("259.30"), response.getTotalFinal());
        assertEquals(3, response.getParcelas());
        assertEquals(new BigDecimal("86.43"), response.getValorParcela());
    }

    @Test
    public void testFretegratisWithExpresa() throws Exception {
        List<Item> items = Arrays.asList(
            new Item("Camiseta", 50.00, 1, 0.30)
        );

        CheckoutRequest request = new CheckoutRequest();
        request.setItens(items);
        request.setModalidadeEntrega("EXPRESSA");
        request.setCupom("FRETEGRATIS");
        request.setFormaPagamento("CARTAO");
        request.setParcelas(1);

        CheckoutResponse response = checkoutService.calculateCheckout(request);

        assertEquals(new BigDecimal("50.00"), response.getSubtotalProdutos());
        BigDecimal expectedShipping = new BigDecimal("26.35");
        assertEquals(0, response.getFrete().compareTo(expectedShipping));
        assertEquals(0, response.getDescontoCupom().compareTo(expectedShipping));
        assertEquals(0, response.getAjustePagamento().compareTo(new BigDecimal("0.00")));
    }
}
