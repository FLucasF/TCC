package com.loja.checkout.service;

import com.loja.checkout.dto.ItemRequest;
import com.loja.checkout.dto.PedidoRequest;
import com.loja.checkout.dto.ResumoResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CheckoutServiceTest {

    private final CheckoutService service = new CheckoutService();

    @Test
    void exemplo1() {
        List<ItemRequest> itens = List.of(
            new ItemRequest("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
            new ItemRequest("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"))
        );

        PedidoRequest pedido = new PedidoRequest(
            itens,
            "EXPRESSA",
            "BEMVINDO10",
            "PIX",
            null,
            "BRONZE",
            "NORTE"
        );

        ResumoResponse resumo = service.calcularResumo(pedido);

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
        List<ItemRequest> itens = List.of(
            new ItemRequest("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
            new ItemRequest("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"))
        );

        PedidoRequest pedido = new PedidoRequest(
            itens,
            "ECONOMICA",
            null,
            "CARTAO",
            6,
            "PRATA",
            "CENTRO_OESTE"
        );

        ResumoResponse resumo = service.calcularResumo(pedido);

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
        List<ItemRequest> itens = List.of(
            new ItemRequest("Fone", new BigDecimal("199.90"), 2, new BigDecimal("0.25"))
        );

        PedidoRequest pedido = new PedidoRequest(
            itens,
            "MOTOBOY",
            "MENOS50",
            "BOLETO",
            null,
            "BRONZE",
            "NORDESTE"
        );

        ResumoResponse resumo = service.calcularResumo(pedido);

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
        List<ItemRequest> itens = List.of(
            new ItemRequest("Meia", new BigDecimal("19.90"), 7, new BigDecimal("0.10")),
            new ItemRequest("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30"))
        );

        PedidoRequest pedido = new PedidoRequest(
            itens,
            "RETIRADA_LOJA",
            "LEVE3PAGUE2",
            "CARTAO",
            3,
            "PRATA",
            "SUL"
        );

        ResumoResponse resumo = service.calcularResumo(pedido);

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
        List<ItemRequest> itens = List.of(
            new ItemRequest("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
            new ItemRequest("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"))
        );

        PedidoRequest pedido = new PedidoRequest(
            itens,
            "EXPRESSA",
            null,
            "PIX",
            null,
            "OURO",
            "SUDESTE"
        );

        ResumoResponse resumo = service.calcularResumo(pedido);

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

    @Test
    void pedidoInvalido_carrinhoVazio() {
        PedidoRequest pedido = new PedidoRequest(
            List.of(),
            "EXPRESSA",
            null,
            "PIX",
            null,
            "BRONZE",
            "SUDESTE"
        );

        CheckoutService.ValidacaoException exception = assertThrows(
            CheckoutService.ValidacaoException.class,
            () -> service.calcularResumo(pedido)
        );

        assertEquals("PEDIDO_INVALIDO", exception.getMessage());
    }

    @Test
    void nivelClubeInvalido() {
        List<ItemRequest> itens = List.of(
            new ItemRequest("Camiseta", new BigDecimal("79.90"), 1, new BigDecimal("0.30"))
        );

        PedidoRequest pedido = new PedidoRequest(
            itens,
            "EXPRESSA",
            null,
            "PIX",
            null,
            "DIAMANTE",
            "SUDESTE"
        );

        CheckoutService.ValidacaoException exception = assertThrows(
            CheckoutService.ValidacaoException.class,
            () -> service.calcularResumo(pedido)
        );

        assertEquals("NIVEL_CLUBE_INVALIDO", exception.getMessage());
    }

    @Test
    void regiaoInvalida() {
        List<ItemRequest> itens = List.of(
            new ItemRequest("Camiseta", new BigDecimal("79.90"), 1, new BigDecimal("0.30"))
        );

        PedidoRequest pedido = new PedidoRequest(
            itens,
            "EXPRESSA",
            null,
            "PIX",
            null,
            "BRONZE",
            "EUROPA"
        );

        CheckoutService.ValidacaoException exception = assertThrows(
            CheckoutService.ValidacaoException.class,
            () -> service.calcularResumo(pedido)
        );

        assertEquals("REGIAO_INVALIDA", exception.getMessage());
    }

    @Test
    void modalidadeInvalida() {
        List<ItemRequest> itens = List.of(
            new ItemRequest("Camiseta", new BigDecimal("79.90"), 1, new BigDecimal("0.30"))
        );

        PedidoRequest pedido = new PedidoRequest(
            itens,
            "DRONE",
            null,
            "PIX",
            null,
            "BRONZE",
            "SUDESTE"
        );

        CheckoutService.ValidacaoException exception = assertThrows(
            CheckoutService.ValidacaoException.class,
            () -> service.calcularResumo(pedido)
        );

        assertEquals("MODALIDADE_INVALIDA", exception.getMessage());
    }

    @Test
    void modalidadeIndisponivel_motoboyAcima5Kg() {
        List<ItemRequest> itens = List.of(
            new ItemRequest("Peso", new BigDecimal("100.00"), 6, new BigDecimal("1.00"))
        );

        PedidoRequest pedido = new PedidoRequest(
            itens,
            "MOTOBOY",
            null,
            "PIX",
            null,
            "BRONZE",
            "SUDESTE"
        );

        CheckoutService.ValidacaoException exception = assertThrows(
            CheckoutService.ValidacaoException.class,
            () -> service.calcularResumo(pedido)
        );

        assertEquals("MODALIDADE_INDISPONIVEL", exception.getMessage());
    }

    @Test
    void cupomInvalido() {
        List<ItemRequest> itens = List.of(
            new ItemRequest("Camiseta", new BigDecimal("79.90"), 1, new BigDecimal("0.30"))
        );

        PedidoRequest pedido = new PedidoRequest(
            itens,
            "EXPRESSA",
            "INEXISTENTE",
            "PIX",
            null,
            "BRONZE",
            "SUDESTE"
        );

        CheckoutService.ValidacaoException exception = assertThrows(
            CheckoutService.ValidacaoException.class,
            () -> service.calcularResumo(pedido)
        );

        assertEquals("CUPOM_INVALIDO", exception.getMessage());
    }

    @Test
    void cupomNaoAplicavel_menos50Abaixo300() {
        List<ItemRequest> itens = List.of(
            new ItemRequest("Camiseta", new BigDecimal("79.90"), 1, new BigDecimal("0.30"))
        );

        PedidoRequest pedido = new PedidoRequest(
            itens,
            "EXPRESSA",
            "MENOS50",
            "PIX",
            null,
            "BRONZE",
            "SUDESTE"
        );

        CheckoutService.ValidacaoException exception = assertThrows(
            CheckoutService.ValidacaoException.class,
            () -> service.calcularResumo(pedido)
        );

        assertEquals("CUPOM_NAO_APLICAVEL", exception.getMessage());
    }

    @Test
    void formaPagamentoInvalida() {
        List<ItemRequest> itens = List.of(
            new ItemRequest("Camiseta", new BigDecimal("79.90"), 1, new BigDecimal("0.30"))
        );

        PedidoRequest pedido = new PedidoRequest(
            itens,
            "EXPRESSA",
            null,
            "CHEQUE",
            null,
            "BRONZE",
            "SUDESTE"
        );

        CheckoutService.ValidacaoException exception = assertThrows(
            CheckoutService.ValidacaoException.class,
            () -> service.calcularResumo(pedido)
        );

        assertEquals("FORMA_PAGAMENTO_INVALIDA", exception.getMessage());
    }

    @Test
    void parcelamentoInvalido_pixComParcelas() {
        List<ItemRequest> itens = List.of(
            new ItemRequest("Camiseta", new BigDecimal("79.90"), 1, new BigDecimal("0.30"))
        );

        PedidoRequest pedido = new PedidoRequest(
            itens,
            "EXPRESSA",
            null,
            "PIX",
            3,
            "BRONZE",
            "SUDESTE"
        );

        CheckoutService.ValidacaoException exception = assertThrows(
            CheckoutService.ValidacaoException.class,
            () -> service.calcularResumo(pedido)
        );

        assertEquals("PARCELAMENTO_INVALIDO", exception.getMessage());
    }

    @Test
    void formaPagamentoIndisponivel_boletoAcima1000() {
        List<ItemRequest> itens = List.of(
            new ItemRequest("Produto Caro", new BigDecimal("1100.00"), 1, new BigDecimal("0.30"))
        );

        PedidoRequest pedido = new PedidoRequest(
            itens,
            "EXPRESSA",
            null,
            "BOLETO",
            null,
            "BRONZE",
            "SUDESTE"
        );

        CheckoutService.ValidacaoException exception = assertThrows(
            CheckoutService.ValidacaoException.class,
            () -> service.calcularResumo(pedido)
        );

        assertEquals("FORMA_PAGAMENTO_INDISPONIVEL", exception.getMessage());
    }
}
