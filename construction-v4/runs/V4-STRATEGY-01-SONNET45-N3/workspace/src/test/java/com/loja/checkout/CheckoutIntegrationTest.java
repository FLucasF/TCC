package com.loja.checkout;

import com.loja.checkout.dto.ItemCarrinho;
import com.loja.checkout.dto.PedidoRequest;
import com.loja.checkout.dto.ResumoResponse;
import com.loja.checkout.servico.CalculadoraResumo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class CheckoutIntegrationTest {

    @Autowired
    private CalculadoraResumo calculadora;

    @Test
    void exemplo1() {
        PedidoRequest pedido = new PedidoRequest(
            List.of(
                new ItemCarrinho("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
                new ItemCarrinho("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"))
            ),
            "EXPRESSA",
            "BEMVINDO10",
            "PIX",
            null,
            "BRONZE",
            "NORTE"
        );

        ResumoResponse resumo = calculadora.calcular(pedido);

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
        PedidoRequest pedido = new PedidoRequest(
            List.of(
                new ItemCarrinho("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
                new ItemCarrinho("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"))
            ),
            "ECONOMICA",
            null,
            "CARTAO",
            6,
            "PRATA",
            "CENTRO_OESTE"
        );

        ResumoResponse resumo = calculadora.calcular(pedido);

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
        PedidoRequest pedido = new PedidoRequest(
            List.of(
                new ItemCarrinho("Fone", new BigDecimal("199.90"), 2, new BigDecimal("0.25"))
            ),
            "MOTOBOY",
            "MENOS50",
            "BOLETO",
            null,
            "BRONZE",
            "NORDESTE"
        );

        ResumoResponse resumo = calculadora.calcular(pedido);

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
        PedidoRequest pedido = new PedidoRequest(
            List.of(
                new ItemCarrinho("Meia", new BigDecimal("19.90"), 7, new BigDecimal("0.10")),
                new ItemCarrinho("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30"))
            ),
            "RETIRADA_LOJA",
            "LEVE3PAGUE2",
            "CARTAO",
            3,
            "PRATA",
            "SUL"
        );

        ResumoResponse resumo = calculadora.calcular(pedido);

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
        PedidoRequest pedido = new PedidoRequest(
            List.of(
                new ItemCarrinho("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
                new ItemCarrinho("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"))
            ),
            "EXPRESSA",
            null,
            "PIX",
            null,
            "OURO",
            "SUDESTE"
        );

        ResumoResponse resumo = calculadora.calcular(pedido);

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
