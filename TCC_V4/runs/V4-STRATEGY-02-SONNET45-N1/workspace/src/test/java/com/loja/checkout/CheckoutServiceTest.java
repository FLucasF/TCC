package com.loja.checkout;

import com.loja.checkout.model.CheckoutRequest;
import com.loja.checkout.model.CheckoutResponse;
import com.loja.checkout.model.ItemCarrinho;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class CheckoutServiceTest {

    @Autowired
    private CheckoutService service;

    @Test
    void exemplo1() {
        List<ItemCarrinho> itens = List.of(
            new ItemCarrinho("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
            new ItemCarrinho("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"))
        );

        CheckoutRequest request = new CheckoutRequest(
            itens,
            "EXPRESSA",
            "BEMVINDO10",
            "PIX",
            null,
            "BRONZE",
            "NORTE"
        );

        CheckoutResponse response = service.calcularResumo(request);

        assertEquals(new BigDecimal("409.70"), response.subtotalProdutos());
        assertEquals(new BigDecimal("40.97"), response.descontoCupom());
        assertEquals(new BigDecimal("33.10"), response.frete());
        assertEquals(2, response.prazoEntregaDias());
        assertEquals(new BigDecimal("10.24"), response.seguro());
        assertEquals(new BigDecimal("-20.60"), response.ajustePagamento());
        assertEquals(new BigDecimal("391.47"), response.totalFinal());
        assertEquals(1, response.parcelas());
        assertEquals(new BigDecimal("391.47"), response.valorParcela());
        assertEquals(new BigDecimal("0.00"), response.creditoProximaCompra());
        assertFalse(response.brinde());
    }

    @Test
    void exemplo2() {
        List<ItemCarrinho> itens = List.of(
            new ItemCarrinho("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
            new ItemCarrinho("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"))
        );

        CheckoutRequest request = new CheckoutRequest(
            itens,
            "ECONOMICA",
            null,
            "CARTAO",
            6,
            "PRATA",
            "CENTRO_OESTE"
        );

        CheckoutResponse response = service.calcularResumo(request);

        assertEquals(new BigDecimal("409.70"), response.subtotalProdutos());
        assertEquals(new BigDecimal("0.00"), response.descontoCupom());
        assertEquals(new BigDecimal("15.60"), response.frete());
        assertEquals(7, response.prazoEntregaDias());
        assertEquals(new BigDecimal("6.15"), response.seguro());
        assertEquals(new BigDecimal("30.55"), response.ajustePagamento());
        assertEquals(new BigDecimal("462.00"), response.totalFinal());
        assertEquals(6, response.parcelas());
        assertEquals(new BigDecimal("77.00"), response.valorParcela());
        assertEquals(new BigDecimal("8.19"), response.creditoProximaCompra());
        assertFalse(response.brinde());
    }

    @Test
    void exemplo3() {
        List<ItemCarrinho> itens = List.of(
            new ItemCarrinho("Fone", new BigDecimal("199.90"), 2, new BigDecimal("0.25"))
        );

        CheckoutRequest request = new CheckoutRequest(
            itens,
            "MOTOBOY",
            "MENOS50",
            "BOLETO",
            null,
            "BRONZE",
            "NORDESTE"
        );

        CheckoutResponse response = service.calcularResumo(request);

        assertEquals(new BigDecimal("399.80"), response.subtotalProdutos());
        assertEquals(new BigDecimal("50.00"), response.descontoCupom());
        assertEquals(new BigDecimal("18.00"), response.frete());
        assertEquals(0, response.prazoEntregaDias());
        assertEquals(new BigDecimal("8.00"), response.seguro());
        assertEquals(new BigDecimal("3.49"), response.ajustePagamento());
        assertEquals(new BigDecimal("379.29"), response.totalFinal());
        assertEquals(1, response.parcelas());
        assertEquals(new BigDecimal("379.29"), response.valorParcela());
        assertEquals(new BigDecimal("0.00"), response.creditoProximaCompra());
        assertFalse(response.brinde());
    }

    @Test
    void exemplo4() {
        List<ItemCarrinho> itens = List.of(
            new ItemCarrinho("Meia", new BigDecimal("19.90"), 7, new BigDecimal("0.10")),
            new ItemCarrinho("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30"))
        );

        CheckoutRequest request = new CheckoutRequest(
            itens,
            "RETIRADA_LOJA",
            "LEVE3PAGUE2",
            "CARTAO",
            3,
            "PRATA",
            "SUL"
        );

        CheckoutResponse response = service.calcularResumo(request);

        assertEquals(new BigDecimal("299.10"), response.subtotalProdutos());
        assertEquals(new BigDecimal("39.80"), response.descontoCupom());
        assertEquals(new BigDecimal("0.00"), response.frete());
        assertEquals(1, response.prazoEntregaDias());
        assertEquals(new BigDecimal("2.99"), response.seguro());
        assertEquals(new BigDecimal("0.00"), response.ajustePagamento());
        assertEquals(new BigDecimal("262.29"), response.totalFinal());
        assertEquals(3, response.parcelas());
        assertEquals(new BigDecimal("87.43"), response.valorParcela());
        assertEquals(new BigDecimal("5.98"), response.creditoProximaCompra());
        assertFalse(response.brinde());
    }

    @Test
    void exemplo5() {
        List<ItemCarrinho> itens = List.of(
            new ItemCarrinho("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
            new ItemCarrinho("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"))
        );

        CheckoutRequest request = new CheckoutRequest(
            itens,
            "EXPRESSA",
            null,
            "PIX",
            null,
            "OURO",
            "SUDESTE"
        );

        CheckoutResponse response = service.calcularResumo(request);

        assertEquals(new BigDecimal("409.70"), response.subtotalProdutos());
        assertEquals(new BigDecimal("0.00"), response.descontoCupom());
        assertEquals(new BigDecimal("0.00"), response.frete());
        assertEquals(2, response.prazoEntregaDias());
        assertEquals(new BigDecimal("4.10"), response.seguro());
        assertEquals(new BigDecimal("-20.69"), response.ajustePagamento());
        assertEquals(new BigDecimal("393.11"), response.totalFinal());
        assertEquals(1, response.parcelas());
        assertEquals(new BigDecimal("393.11"), response.valorParcela());
        assertEquals(new BigDecimal("20.48"), response.creditoProximaCompra());
        assertFalse(response.brinde());
    }
}
