package com.loja.checkout;

import com.loja.checkout.dto.ItemPedido;
import com.loja.checkout.dto.PedidoRequest;
import com.loja.checkout.dto.ResumoResponse;
import com.loja.checkout.service.CheckoutService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CheckoutServiceTest {

    private final CheckoutService service = new CheckoutService();

    @Test
    void exemplo1() {
        var itens = List.of(
            new ItemPedido("Camiseta", 79.90, 2, 0.30),
            new ItemPedido("Tênis", 249.90, 1, 1.20)
        );

        var pedido = new PedidoRequest(
            itens,
            "EXPRESSA",
            "BEMVINDO10",
            "PIX",
            null,
            "BRONZE",
            "NORTE"
        );

        ResumoResponse resumo = service.calcularResumo(pedido);

        assertEquals(409.70, resumo.subtotalProdutos());
        assertEquals(40.97, resumo.descontoCupom());
        assertEquals(33.10, resumo.frete());
        assertEquals(2, resumo.prazoEntregaDias());
        assertEquals(10.24, resumo.seguro());
        assertEquals(-20.60, resumo.ajustePagamento());
        assertEquals(391.47, resumo.totalFinal());
        assertEquals(1, resumo.parcelas());
        assertEquals(391.47, resumo.valorParcela());
        assertEquals(0.00, resumo.creditoProximaCompra());
        assertFalse(resumo.brinde());
    }

    @Test
    void exemplo2() {
        var itens = List.of(
            new ItemPedido("Camiseta", 79.90, 2, 0.30),
            new ItemPedido("Tênis", 249.90, 1, 1.20)
        );

        var pedido = new PedidoRequest(
            itens,
            "ECONOMICA",
            null,
            "CARTAO",
            6,
            "PRATA",
            "CENTRO_OESTE"
        );

        ResumoResponse resumo = service.calcularResumo(pedido);

        assertEquals(409.70, resumo.subtotalProdutos());
        assertEquals(0.00, resumo.descontoCupom());
        assertEquals(15.60, resumo.frete());
        assertEquals(7, resumo.prazoEntregaDias());
        assertEquals(6.15, resumo.seguro());
        assertEquals(30.55, resumo.ajustePagamento());
        assertEquals(462.00, resumo.totalFinal());
        assertEquals(6, resumo.parcelas());
        assertEquals(77.00, resumo.valorParcela());
        assertEquals(8.19, resumo.creditoProximaCompra());
        assertFalse(resumo.brinde());
    }

    @Test
    void exemplo3() {
        var itens = List.of(
            new ItemPedido("Fone", 199.90, 2, 0.25)
        );

        var pedido = new PedidoRequest(
            itens,
            "MOTOBOY",
            "MENOS50",
            "BOLETO",
            null,
            "BRONZE",
            "NORDESTE"
        );

        ResumoResponse resumo = service.calcularResumo(pedido);

        assertEquals(399.80, resumo.subtotalProdutos());
        assertEquals(50.00, resumo.descontoCupom());
        assertEquals(18.00, resumo.frete());
        assertEquals(0, resumo.prazoEntregaDias());
        assertEquals(8.00, resumo.seguro());
        assertEquals(3.49, resumo.ajustePagamento());
        assertEquals(379.29, resumo.totalFinal());
        assertEquals(1, resumo.parcelas());
        assertEquals(379.29, resumo.valorParcela());
        assertEquals(0.00, resumo.creditoProximaCompra());
        assertFalse(resumo.brinde());
    }

    @Test
    void exemplo4() {
        var itens = List.of(
            new ItemPedido("Meia", 19.90, 7, 0.10),
            new ItemPedido("Camiseta", 79.90, 2, 0.30)
        );

        var pedido = new PedidoRequest(
            itens,
            "RETIRADA_LOJA",
            "LEVE3PAGUE2",
            "CARTAO",
            3,
            "PRATA",
            "SUL"
        );

        ResumoResponse resumo = service.calcularResumo(pedido);

        assertEquals(299.10, resumo.subtotalProdutos());
        assertEquals(39.80, resumo.descontoCupom());
        assertEquals(0.00, resumo.frete());
        assertEquals(1, resumo.prazoEntregaDias());
        assertEquals(2.99, resumo.seguro());
        assertEquals(0.00, resumo.ajustePagamento());
        assertEquals(262.29, resumo.totalFinal());
        assertEquals(3, resumo.parcelas());
        assertEquals(87.43, resumo.valorParcela());
        assertEquals(5.98, resumo.creditoProximaCompra());
        assertFalse(resumo.brinde());
    }

    @Test
    void exemplo5() {
        var itens = List.of(
            new ItemPedido("Camiseta", 79.90, 2, 0.30),
            new ItemPedido("Tênis", 249.90, 1, 1.20)
        );

        var pedido = new PedidoRequest(
            itens,
            "EXPRESSA",
            null,
            "PIX",
            null,
            "OURO",
            "SUDESTE"
        );

        ResumoResponse resumo = service.calcularResumo(pedido);

        assertEquals(409.70, resumo.subtotalProdutos());
        assertEquals(0.00, resumo.descontoCupom());
        assertEquals(0.00, resumo.frete());
        assertEquals(2, resumo.prazoEntregaDias());
        assertEquals(4.10, resumo.seguro());
        assertEquals(-20.69, resumo.ajustePagamento());
        assertEquals(393.11, resumo.totalFinal());
        assertEquals(1, resumo.parcelas());
        assertEquals(393.11, resumo.valorParcela());
        assertEquals(20.48, resumo.creditoProximaCompra());
        assertFalse(resumo.brinde());
    }
}
