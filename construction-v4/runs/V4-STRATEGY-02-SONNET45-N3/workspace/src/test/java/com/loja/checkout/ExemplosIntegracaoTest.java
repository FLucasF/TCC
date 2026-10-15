package com.loja.checkout;

import com.loja.checkout.domain.PedidoRequest;
import com.loja.checkout.domain.ResumoResponse;
import com.loja.checkout.service.CalculadoraResumo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ExemplosIntegracaoTest {

    @Autowired
    private CalculadoraResumo calculadora;

    @Test
    void exemplo1() {
        var request = new PedidoRequest(
            List.of(
                new PedidoRequest.ItemCarrinho("Camiseta", 79.90, 2, 0.30),
                new PedidoRequest.ItemCarrinho("Tênis", 249.90, 1, 1.20)
            ),
            "EXPRESSA",
            "BEMVINDO10",
            "PIX",
            null,
            "BRONZE",
            "NORTE"
        );

        ResumoResponse resultado = (ResumoResponse) calculadora.calcular(request);

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
        var request = new PedidoRequest(
            List.of(
                new PedidoRequest.ItemCarrinho("Camiseta", 79.90, 2, 0.30),
                new PedidoRequest.ItemCarrinho("Tênis", 249.90, 1, 1.20)
            ),
            "ECONOMICA",
            null,
            "CARTAO",
            6,
            "PRATA",
            "CENTRO_OESTE"
        );

        ResumoResponse resultado = (ResumoResponse) calculadora.calcular(request);

        assertEquals(new BigDecimal("409.70"), resultado.subtotalProdutos());
        assertEquals(BigDecimal.ZERO.setScale(2), resultado.descontoCupom());
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
        var request = new PedidoRequest(
            List.of(
                new PedidoRequest.ItemCarrinho("Fone", 199.90, 2, 0.25)
            ),
            "MOTOBOY",
            "MENOS50",
            "BOLETO",
            null,
            "BRONZE",
            "NORDESTE"
        );

        ResumoResponse resultado = (ResumoResponse) calculadora.calcular(request);

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
        var request = new PedidoRequest(
            List.of(
                new PedidoRequest.ItemCarrinho("Meia", 19.90, 7, 0.10),
                new PedidoRequest.ItemCarrinho("Camiseta", 79.90, 2, 0.30)
            ),
            "RETIRADA_LOJA",
            "LEVE3PAGUE2",
            "CARTAO",
            3,
            "PRATA",
            "SUL"
        );

        ResumoResponse resultado = (ResumoResponse) calculadora.calcular(request);

        assertEquals(new BigDecimal("299.10"), resultado.subtotalProdutos());
        assertEquals(new BigDecimal("39.80"), resultado.descontoCupom());
        assertEquals(BigDecimal.ZERO.setScale(2), resultado.frete());
        assertEquals(1, resultado.prazoEntregaDias());
        assertEquals(new BigDecimal("2.99"), resultado.seguro());
        assertEquals(BigDecimal.ZERO.setScale(2), resultado.ajustePagamento());
        assertEquals(new BigDecimal("262.29"), resultado.totalFinal());
        assertEquals(3, resultado.parcelas());
        assertEquals(new BigDecimal("87.43"), resultado.valorParcela());
        assertEquals(new BigDecimal("5.98"), resultado.creditoProximaCompra());
        assertFalse(resultado.brinde());
    }

    @Test
    void exemplo5() {
        var request = new PedidoRequest(
            List.of(
                new PedidoRequest.ItemCarrinho("Camiseta", 79.90, 2, 0.30),
                new PedidoRequest.ItemCarrinho("Tênis", 249.90, 1, 1.20)
            ),
            "EXPRESSA",
            null,
            "PIX",
            null,
            "OURO",
            "SUDESTE"
        );

        ResumoResponse resultado = (ResumoResponse) calculadora.calcular(request);

        assertEquals(new BigDecimal("409.70"), resultado.subtotalProdutos());
        assertEquals(BigDecimal.ZERO.setScale(2), resultado.descontoCupom());
        assertEquals(BigDecimal.ZERO.setScale(2), resultado.frete());
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
