package com.loja.checkout.service;

import com.loja.checkout.dto.PedidoRequest;
import com.loja.checkout.dto.ResumoResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CalculadorResumoTest {

    private CalculadorResumo calculador;

    @BeforeEach
    void setUp() {
        calculador = new CalculadorResumo();
    }

    @Test
    void exemplo1() {
        // Camiseta 79,90 × 2 (0,30 kg) + Tênis 249,90 × 1 (1,20 kg)
        // EXPRESSA, cupom BEMVINDO10, PIX, clube BRONZE, região NORTE
        List<PedidoRequest.Item> itens = Arrays.asList(
            new PedidoRequest.Item("Camiseta", 79.90, 2, 0.30),
            new PedidoRequest.Item("Tênis", 249.90, 1, 1.20)
        );

        PedidoRequest request = new PedidoRequest(
            itens,
            "EXPRESSA",
            "BEMVINDO10",
            "PIX",
            1,
            "BRONZE",
            "NORTE"
        );

        ResumoResponse resumo = calculador.calcular(request);

        assertEquals(new BigDecimal("409.70"), resumo.subtotalProdutos());
        assertEquals(new BigDecimal("40.97"), resumo.descontoCupom());
        assertEquals(new BigDecimal("33.10"), resumo.frete());
        assertEquals(2, resumo.prazoEntregaDias());
        assertEquals(new BigDecimal("10.24"), resumo.seguro());
        assertEquals(new BigDecimal("-20.60"), resumo.ajustePagamento());
        assertEquals(new BigDecimal("391.47"), resumo.totalFinal());
        assertEquals(1, resumo.parcelas());
        assertEquals(new BigDecimal("391.47"), resumo.valorParcela());
        assertEquals(new BigDecimal("0.00"), resumo.creditoProximaCompra());
        assertFalse(resumo.brinde());
    }

    @Test
    void exemplo2() {
        // Camiseta 79,90 × 2 (0,30 kg) + Tênis 249,90 × 1 (1,20 kg)
        // ECONOMICA, sem cupom, CARTAO em 6×, clube PRATA, região CENTRO_OESTE
        List<PedidoRequest.Item> itens = Arrays.asList(
            new PedidoRequest.Item("Camiseta", 79.90, 2, 0.30),
            new PedidoRequest.Item("Tênis", 249.90, 1, 1.20)
        );

        PedidoRequest request = new PedidoRequest(
            itens,
            "ECONOMICA",
            null,
            "CARTAO",
            6,
            "PRATA",
            "CENTRO_OESTE"
        );

        ResumoResponse resumo = calculador.calcular(request);

        assertEquals(new BigDecimal("409.70"), resumo.subtotalProdutos());
        assertEquals(new BigDecimal("0.00"), resumo.descontoCupom());
        assertEquals(new BigDecimal("15.60"), resumo.frete());
        assertEquals(7, resumo.prazoEntregaDias());
        assertEquals(new BigDecimal("6.15"), resumo.seguro());
        assertEquals(new BigDecimal("30.55"), resumo.ajustePagamento());
        assertEquals(new BigDecimal("462.00"), resumo.totalFinal());
        assertEquals(6, resumo.parcelas());
        assertEquals(new BigDecimal("77.00"), resumo.valorParcela());
        assertEquals(new BigDecimal("8.19"), resumo.creditoProximaCompra());
        assertFalse(resumo.brinde());
    }

    @Test
    void exemplo3() {
        // Fone 199,90 × 2 (0,25 kg)
        // MOTOBOY, cupom MENOS50, BOLETO, clube BRONZE, região NORDESTE
        List<PedidoRequest.Item> itens = Arrays.asList(
            new PedidoRequest.Item("Fone", 199.90, 2, 0.25)
        );

        PedidoRequest request = new PedidoRequest(
            itens,
            "MOTOBOY",
            "MENOS50",
            "BOLETO",
            1,
            "BRONZE",
            "NORDESTE"
        );

        ResumoResponse resumo = calculador.calcular(request);

        assertEquals(new BigDecimal("399.80"), resumo.subtotalProdutos());
        assertEquals(new BigDecimal("50.00"), resumo.descontoCupom());
        assertEquals(new BigDecimal("18.00"), resumo.frete());
        assertEquals(0, resumo.prazoEntregaDias());
        assertEquals(new BigDecimal("8.00"), resumo.seguro());
        assertEquals(new BigDecimal("3.49"), resumo.ajustePagamento());
        assertEquals(new BigDecimal("379.29"), resumo.totalFinal());
        assertEquals(1, resumo.parcelas());
        assertEquals(new BigDecimal("379.29"), resumo.valorParcela());
        assertEquals(new BigDecimal("0.00"), resumo.creditoProximaCompra());
        assertFalse(resumo.brinde());
    }

    @Test
    void exemplo4() {
        // Meia 19,90 × 7 (0,10 kg) + Camiseta 79,90 × 2 (0,30 kg)
        // RETIRADA_LOJA, cupom LEVE3PAGUE2, CARTAO em 3×, clube PRATA, região SUL
        List<PedidoRequest.Item> itens = Arrays.asList(
            new PedidoRequest.Item("Meia", 19.90, 7, 0.10),
            new PedidoRequest.Item("Camiseta", 79.90, 2, 0.30)
        );

        PedidoRequest request = new PedidoRequest(
            itens,
            "RETIRADA_LOJA",
            "LEVE3PAGUE2",
            "CARTAO",
            3,
            "PRATA",
            "SUL"
        );

        ResumoResponse resumo = calculador.calcular(request);

        assertEquals(new BigDecimal("299.10"), resumo.subtotalProdutos());
        assertEquals(new BigDecimal("39.80"), resumo.descontoCupom());
        assertEquals(new BigDecimal("0.00"), resumo.frete());
        assertEquals(1, resumo.prazoEntregaDias());
        assertEquals(new BigDecimal("2.99"), resumo.seguro());
        assertEquals(new BigDecimal("0.00"), resumo.ajustePagamento());
        assertEquals(new BigDecimal("262.29"), resumo.totalFinal());
        assertEquals(3, resumo.parcelas());
        assertEquals(new BigDecimal("87.43"), resumo.valorParcela());
        assertEquals(new BigDecimal("5.98"), resumo.creditoProximaCompra());
        assertFalse(resumo.brinde());
    }

    @Test
    void exemplo5() {
        // Camiseta 79,90 × 2 (0,30 kg) + Tênis 249,90 × 1 (1,20 kg)
        // EXPRESSA, sem cupom, PIX, clube OURO, região SUDESTE
        List<PedidoRequest.Item> itens = Arrays.asList(
            new PedidoRequest.Item("Camiseta", 79.90, 2, 0.30),
            new PedidoRequest.Item("Tênis", 249.90, 1, 1.20)
        );

        PedidoRequest request = new PedidoRequest(
            itens,
            "EXPRESSA",
            null,
            "PIX",
            1,
            "OURO",
            "SUDESTE"
        );

        ResumoResponse resumo = calculador.calcular(request);

        assertEquals(new BigDecimal("409.70"), resumo.subtotalProdutos());
        assertEquals(new BigDecimal("0.00"), resumo.descontoCupom());
        assertEquals(new BigDecimal("0.00"), resumo.frete());
        assertEquals(2, resumo.prazoEntregaDias());
        assertEquals(new BigDecimal("4.10"), resumo.seguro());
        assertEquals(new BigDecimal("-20.69"), resumo.ajustePagamento());
        assertEquals(new BigDecimal("393.11"), resumo.totalFinal());
        assertEquals(1, resumo.parcelas());
        assertEquals(new BigDecimal("393.11"), resumo.valorParcela());
        assertEquals(new BigDecimal("20.48"), resumo.creditoProximaCompra());
        assertFalse(resumo.brinde());
    }
}
