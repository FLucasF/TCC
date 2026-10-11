package com.loja.checkout.service;

import com.loja.checkout.dto.ItemCarrinho;
import com.loja.checkout.dto.PedidoRequest;
import com.loja.checkout.dto.ResumoResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CalculadoraResumoServiceTest {
    private CalculadoraResumoService service;

    @BeforeEach
    void setUp() {
        service = new CalculadoraResumoService();
    }

    @Test
    void exemplo1() {
        // Camiseta 79,90 × 2 (0,30 kg) + Tênis 249,90 × 1 (1,20 kg), EXPRESSA, BEMVINDO10, PIX, BRONZE, NORTE
        // → subtotal 409,70 · cupom 40,97 · frete 33,10 · prazo 2 · seguro 10,24 · ajuste −20,60 · total final 391,47 · 1× de 391,47 · crédito 0,00 · brinde não

        PedidoRequest pedido = new PedidoRequest(
            List.of(
                new ItemCarrinho("Camiseta", 79.90, 2, 0.30),
                new ItemCarrinho("Tênis", 249.90, 1, 1.20)
            ),
            "EXPRESSA",
            "BEMVINDO10",
            "PIX",
            null,
            "BRONZE",
            "NORTE"
        );

        ResumoResponse resultado = service.calcular(pedido);

        assertEquals(new BigDecimal("409.70"), resultado.subtotalProdutos());
        assertEquals(new BigDecimal("40.97"), resultado.descontoCupom());
        assertEquals(new BigDecimal("33.10"), resultado.frete());
        assertEquals(2, resultado.prazoEntregaDias());
        assertEquals(new BigDecimal("10.24"), resultado.seguro());
        assertEquals(new BigDecimal("-20.60"), resultado.ajustePagamento());
        assertEquals(new BigDecimal("391.47"), resultado.totalFinal());
        assertEquals(1, resultado.parcelas());
        assertEquals(new BigDecimal("391.47"), resultado.valorParcela());
        assertEquals(new BigDecimal("0.00"), resultado.creditoProximaCompra());
        assertFalse(resultado.brinde());
    }

    @Test
    void exemplo2() {
        // mesmos itens, ECONOMICA, sem cupom, CARTAO em 6×, PRATA, CENTRO_OESTE
        // → subtotal 409,70 · cupom 0,00 · frete 15,60 · prazo 7 · seguro 6,15 · ajuste 30,55 · total final 462,00 · 6× de 77,00 · crédito 8,19 · brinde não

        PedidoRequest pedido = new PedidoRequest(
            List.of(
                new ItemCarrinho("Camiseta", 79.90, 2, 0.30),
                new ItemCarrinho("Tênis", 249.90, 1, 1.20)
            ),
            "ECONOMICA",
            null,
            "CARTAO",
            6,
            "PRATA",
            "CENTRO_OESTE"
        );

        ResumoResponse resultado = service.calcular(pedido);

        assertEquals(new BigDecimal("409.70"), resultado.subtotalProdutos());
        assertEquals(new BigDecimal("0.00"), resultado.descontoCupom());
        assertEquals(new BigDecimal("15.60"), resultado.frete());
        assertEquals(7, resultado.prazoEntregaDias());
        assertEquals(new BigDecimal("6.15"), resultado.seguro());
        assertEquals(new BigDecimal("30.55"), resultado.ajustePagamento());
        assertEquals(new BigDecimal("462.00"), resultado.totalFinal());
        assertEquals(6, resultado.parcelas());
        assertEquals(new BigDecimal("77.00"), resultado.valorParcela());
        assertEquals(new BigDecimal("8.19"), resultado.creditoProximaCompra());
        assertFalse(resultado.brinde());
    }

    @Test
    void exemplo3() {
        // Fone 199,90 × 2 (0,25 kg), MOTOBOY, MENOS50, BOLETO, BRONZE, NORDESTE
        // → subtotal 399,80 · cupom 50,00 · frete 18,00 · prazo 0 · seguro 8,00 · ajuste 3,49 · total final 379,29 · 1× de 379,29 · crédito 0,00 · brinde não

        PedidoRequest pedido = new PedidoRequest(
            List.of(
                new ItemCarrinho("Fone", 199.90, 2, 0.25)
            ),
            "MOTOBOY",
            "MENOS50",
            "BOLETO",
            null,
            "BRONZE",
            "NORDESTE"
        );

        ResumoResponse resultado = service.calcular(pedido);

        assertEquals(new BigDecimal("399.80"), resultado.subtotalProdutos());
        assertEquals(new BigDecimal("50.00"), resultado.descontoCupom());
        assertEquals(new BigDecimal("18.00"), resultado.frete());
        assertEquals(0, resultado.prazoEntregaDias());
        assertEquals(new BigDecimal("8.00"), resultado.seguro());
        assertEquals(new BigDecimal("3.49"), resultado.ajustePagamento());
        assertEquals(new BigDecimal("379.29"), resultado.totalFinal());
        assertEquals(1, resultado.parcelas());
        assertEquals(new BigDecimal("379.29"), resultado.valorParcela());
        assertEquals(new BigDecimal("0.00"), resultado.creditoProximaCompra());
        assertFalse(resultado.brinde());
    }

    @Test
    void exemplo4() {
        // Meia 19,90 × 7 (0,10 kg) + Camiseta 79,90 × 2 (0,30 kg), RETIRADA_LOJA, LEVE3PAGUE2, CARTAO em 3×, PRATA, SUL
        // → subtotal 299,10 · cupom 39,80 · frete 0,00 · prazo 1 · seguro 2,99 · ajuste 0,00 · total final 262,29 · 3× de 87,43 · crédito 5,98 · brinde não

        PedidoRequest pedido = new PedidoRequest(
            List.of(
                new ItemCarrinho("Meia", 19.90, 7, 0.10),
                new ItemCarrinho("Camiseta", 79.90, 2, 0.30)
            ),
            "RETIRADA_LOJA",
            "LEVE3PAGUE2",
            "CARTAO",
            3,
            "PRATA",
            "SUL"
        );

        ResumoResponse resultado = service.calcular(pedido);

        assertEquals(new BigDecimal("299.10"), resultado.subtotalProdutos());
        assertEquals(new BigDecimal("39.80"), resultado.descontoCupom());
        assertEquals(new BigDecimal("0.00"), resultado.frete());
        assertEquals(1, resultado.prazoEntregaDias());
        assertEquals(new BigDecimal("2.99"), resultado.seguro());
        assertEquals(new BigDecimal("0.00"), resultado.ajustePagamento());
        assertEquals(new BigDecimal("262.29"), resultado.totalFinal());
        assertEquals(3, resultado.parcelas());
        assertEquals(new BigDecimal("87.43"), resultado.valorParcela());
        assertEquals(new BigDecimal("5.98"), resultado.creditoProximaCompra());
        assertFalse(resultado.brinde());
    }

    @Test
    void exemplo5() {
        // Camiseta 79,90 × 2 (0,30 kg) + Tênis 249,90 × 1 (1,20 kg), EXPRESSA, sem cupom, PIX, OURO, SUDESTE
        // → subtotal 409,70 · cupom 0,00 · frete 0,00 (OURO não paga) · prazo 2 · seguro 4,10 · ajuste −20,69 · total final 393,11 · 1× de 393,11 · crédito 20,48 · brinde não

        PedidoRequest pedido = new PedidoRequest(
            List.of(
                new ItemCarrinho("Camiseta", 79.90, 2, 0.30),
                new ItemCarrinho("Tênis", 249.90, 1, 1.20)
            ),
            "EXPRESSA",
            null,
            "PIX",
            null,
            "OURO",
            "SUDESTE"
        );

        ResumoResponse resultado = service.calcular(pedido);

        assertEquals(new BigDecimal("409.70"), resultado.subtotalProdutos());
        assertEquals(new BigDecimal("0.00"), resultado.descontoCupom());
        assertEquals(new BigDecimal("0.00"), resultado.frete());
        assertEquals(2, resultado.prazoEntregaDias());
        assertEquals(new BigDecimal("4.10"), resultado.seguro());
        assertEquals(new BigDecimal("-20.69"), resultado.ajustePagamento());
        assertEquals(new BigDecimal("393.11"), resultado.totalFinal());
        assertEquals(1, resultado.parcelas());
        assertEquals(new BigDecimal("393.11"), resultado.valorParcela());
        assertEquals(new BigDecimal("20.48"), resultado.creditoProximaCompra());
        assertFalse(resultado.brinde());
    }
}
