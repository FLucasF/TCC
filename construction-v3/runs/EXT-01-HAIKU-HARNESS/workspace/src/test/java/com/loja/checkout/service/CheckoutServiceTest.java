package com.loja.checkout.service;

import com.loja.checkout.api.ResumoRequest;
import com.loja.checkout.api.ResumoResponse;
import com.loja.checkout.model.Item;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class CheckoutServiceTest {

    private final CheckoutService service = new CheckoutService();

    @Test
    void exemplo1() {
        var request = new ResumoRequest(
            Arrays.asList(
                new Item("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
                new Item("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"))
            ),
            "EXPRESSA",
            "BEMVINDO10",
            "PIX",
            1,
            "BRONZE",
            "SUDESTE"
        );

        ResumoResponse response = service.calcularResumo(request);

        assertEquals(new BigDecimal("409.70"), response.subtotalProdutos());
        assertEquals(new BigDecimal("40.97"), response.descontoCupom());
        assertEquals(new BigDecimal("33.10"), response.frete());
        assertEquals(2, response.prazoEntregaDias());
        assertEquals(new BigDecimal("381.74"), response.totalFinal());
        assertEquals(1, response.parcelas());
        assertEquals(new BigDecimal("381.74"), response.valorParcela());
    }

    @Test
    void exemplo2() {
        var request = new ResumoRequest(
            Arrays.asList(
                new Item("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
                new Item("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"))
            ),
            "ECONOMICA",
            null,
            "CARTAO",
            6,
            "BRONZE",
            "SUDESTE"
        );

        ResumoResponse response = service.calcularResumo(request);

        assertEquals(new BigDecimal("409.70"), response.subtotalProdutos());
        assertEquals(new BigDecimal("0.00"), response.descontoCupom());
        assertEquals(new BigDecimal("15.60"), response.frete());
        assertEquals(7, response.prazoEntregaDias());
        assertEquals(new BigDecimal("455.40"), response.totalFinal());
        assertEquals(6, response.parcelas());
        assertEquals(new BigDecimal("75.90"), response.valorParcela());
    }

    @Test
    void exemplo3() {
        var request = new ResumoRequest(
            Arrays.asList(
                new Item("Fone", new BigDecimal("199.90"), 2, new BigDecimal("0.25"))
            ),
            "MOTOBOY",
            "MENOS50",
            "BOLETO",
            1,
            "BRONZE",
            "SUDESTE"
        );

        ResumoResponse response = service.calcularResumo(request);

        assertEquals(new BigDecimal("399.80"), response.subtotalProdutos());
        assertEquals(new BigDecimal("50.00"), response.descontoCupom());
        assertEquals(new BigDecimal("18.00"), response.frete());
        assertEquals(0, response.prazoEntregaDias());
        assertEquals(new BigDecimal("371.29"), response.totalFinal());
        assertEquals(1, response.parcelas());
        assertEquals(new BigDecimal("371.29"), response.valorParcela());
    }

    @Test
    void exemplo4() {
        var request = new ResumoRequest(
            Arrays.asList(
                new Item("Meia", new BigDecimal("19.90"), 7, new BigDecimal("0.10")),
                new Item("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30"))
            ),
            "RETIRADA_LOJA",
            "LEVE3PAGUE2",
            "CARTAO",
            3,
            "BRONZE",
            "SUDESTE"
        );

        ResumoResponse response = service.calcularResumo(request);

        assertEquals(new BigDecimal("299.10"), response.subtotalProdutos());
        assertEquals(new BigDecimal("39.80"), response.descontoCupom());
        assertEquals(new BigDecimal("0.00"), response.frete());
        assertEquals(1, response.prazoEntregaDias());
        assertEquals(new BigDecimal("259.29"), response.totalFinal());
        assertEquals(3, response.parcelas());
        assertEquals(new BigDecimal("86.43"), response.valorParcela());
    }

    @Test
    void erroCarrinhoVazio() {
        var request = new ResumoRequest(
            Arrays.asList(),
            "EXPRESSA",
            null,
            "PIX",
            1,
            "BRONZE",
            "SUDESTE"
        );

        assertThrows(ErroCheckout.class, () -> service.calcularResumo(request));
    }

    @Test
    void erroNivelClubeInvalido() {
        var request = new ResumoRequest(
            Arrays.asList(
                new Item("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30"))
            ),
            "EXPRESSA",
            null,
            "PIX",
            1,
            "INVALIDO",
            "SUDESTE"
        );

        assertThrows(ErroCheckout.class, () -> service.calcularResumo(request));
    }

    @Test
    void erroRegiaInvalida() {
        var request = new ResumoRequest(
            Arrays.asList(
                new Item("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30"))
            ),
            "EXPRESSA",
            null,
            "PIX",
            1,
            "BRONZE",
            "INVALIDA"
        );

        assertThrows(ErroCheckout.class, () -> service.calcularResumo(request));
    }

    @Test
    void erroModalidadeInvalida() {
        var request = new ResumoRequest(
            Arrays.asList(
                new Item("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30"))
            ),
            "INVALIDA",
            null,
            "PIX",
            1,
            "BRONZE",
            "SUDESTE"
        );

        assertThrows(ErroCheckout.class, () -> service.calcularResumo(request));
    }

    @Test
    void erroMotoboySobreoPeso() {
        var request = new ResumoRequest(
            Arrays.asList(
                new Item("Sofá", new BigDecimal("1000"), 1, new BigDecimal("6"))
            ),
            "MOTOBOY",
            null,
            "PIX",
            1,
            "BRONZE",
            "SUDESTE"
        );

        assertThrows(ErroCheckout.class, () -> service.calcularResumo(request));
    }

    @Test
    void erroCupomInvalido() {
        var request = new ResumoRequest(
            Arrays.asList(
                new Item("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30"))
            ),
            "EXPRESSA",
            "INVALIDO",
            "PIX",
            1,
            "BRONZE",
            "SUDESTE"
        );

        assertThrows(ErroCheckout.class, () -> service.calcularResumo(request));
    }

    @Test
    void erroCupomMenos50AbaixoDolimite() {
        var request = new ResumoRequest(
            Arrays.asList(
                new Item("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30"))
            ),
            "EXPRESSA",
            "MENOS50",
            "PIX",
            1,
            "BRONZE",
            "SUDESTE"
        );

        assertThrows(ErroCheckout.class, () -> service.calcularResumo(request));
    }

    @Test
    void erroFormaPagamentoInvalida() {
        var request = new ResumoRequest(
            Arrays.asList(
                new Item("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30"))
            ),
            "EXPRESSA",
            null,
            "INVALIDA",
            1,
            "BRONZE",
            "SUDESTE"
        );

        assertThrows(ErroCheckout.class, () -> service.calcularResumo(request));
    }

    @Test
    void erroParcelamentoInvalido() {
        var request = new ResumoRequest(
            Arrays.asList(
                new Item("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30"))
            ),
            "EXPRESSA",
            null,
            "PIX",
            2,
            "BRONZE",
            "SUDESTE"
        );

        assertThrows(ErroCheckout.class, () -> service.calcularResumo(request));
    }

    @Test
    void erroBoletoAcimaDoLimite() {
        var request = new ResumoRequest(
            Arrays.asList(
                new Item("Produto", new BigDecimal("1000"), 2, new BigDecimal("0.30"))
            ),
            "EXPRESSA",
            null,
            "BOLETO",
            1,
            "BRONZE",
            "SUDESTE"
        );

        assertThrows(ErroCheckout.class, () -> service.calcularResumo(request));
    }
}
