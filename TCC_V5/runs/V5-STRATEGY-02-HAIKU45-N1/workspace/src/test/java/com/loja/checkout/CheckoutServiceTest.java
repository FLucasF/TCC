package com.loja.checkout;

import com.loja.checkout.dto.CheckoutRequest;
import com.loja.checkout.dto.CheckoutResponse;
import com.loja.checkout.dto.ItemRequest;
import com.loja.checkout.enums.FormaPagamento;
import com.loja.checkout.enums.ModalidadeEntrega;
import com.loja.checkout.enums.NivelClube;
import com.loja.checkout.enums.Regiao;
import com.loja.checkout.service.CheckoutService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CheckoutServiceTest {

    private CheckoutService service;

    @BeforeEach
    void setup() {
        service = new CheckoutService();
    }

    @Test
    void exemplo1() {
        CheckoutRequest request = new CheckoutRequest(
            List.of(
                new ItemRequest("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
                new ItemRequest("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"))
            ),
            ModalidadeEntrega.EXPRESSA,
            "BEMVINDO10",
            FormaPagamento.PIX,
            1,
            NivelClube.BRONZE,
            Regiao.NORTE
        );

        Object resultado = service.processar(request);
        assertTrue(resultado instanceof CheckoutResponse);

        CheckoutResponse response = (CheckoutResponse) resultado;
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
        CheckoutRequest request = new CheckoutRequest(
            List.of(
                new ItemRequest("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
                new ItemRequest("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"))
            ),
            ModalidadeEntrega.ECONOMICA,
            null,
            FormaPagamento.CARTAO,
            6,
            NivelClube.PRATA,
            Regiao.CENTRO_OESTE
        );

        Object resultado = service.processar(request);
        assertTrue(resultado instanceof CheckoutResponse);

        CheckoutResponse response = (CheckoutResponse) resultado;
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
        CheckoutRequest request = new CheckoutRequest(
            List.of(
                new ItemRequest("Fone", new BigDecimal("199.90"), 2, new BigDecimal("0.25"))
            ),
            ModalidadeEntrega.MOTOBOY,
            "MENOS50",
            FormaPagamento.BOLETO,
            1,
            NivelClube.BRONZE,
            Regiao.NORDESTE
        );

        Object resultado = service.processar(request);
        assertTrue(resultado instanceof CheckoutResponse);

        CheckoutResponse response = (CheckoutResponse) resultado;
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
        CheckoutRequest request = new CheckoutRequest(
            List.of(
                new ItemRequest("Meia", new BigDecimal("19.90"), 7, new BigDecimal("0.10")),
                new ItemRequest("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30"))
            ),
            ModalidadeEntrega.RETIRADA_LOJA,
            "LEVE3PAGUE2",
            FormaPagamento.CARTAO,
            3,
            NivelClube.PRATA,
            Regiao.SUL
        );

        Object resultado = service.processar(request);
        assertTrue(resultado instanceof CheckoutResponse);

        CheckoutResponse response = (CheckoutResponse) resultado;
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
        CheckoutRequest request = new CheckoutRequest(
            List.of(
                new ItemRequest("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
                new ItemRequest("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"))
            ),
            ModalidadeEntrega.EXPRESSA,
            null,
            FormaPagamento.PIX,
            1,
            NivelClube.OURO,
            Regiao.SUDESTE
        );

        Object resultado = service.processar(request);
        assertTrue(resultado instanceof CheckoutResponse);

        CheckoutResponse response = (CheckoutResponse) resultado;
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
