package com.loja.checkout;

import com.loja.checkout.dto.*;
import com.loja.checkout.servico.CheckoutService;
import com.loja.checkout.servico.ErroCheckoutException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CheckoutErrosTest {

    private final CheckoutService service = new CheckoutService();

    @Test
    void erroCarrinhoVazio() {
        PedidoRequest pedido = new PedidoRequest(
            new ArrayList<>(),
            "ECONOMICA",
            null,
            "PIX",
            1,
            "BRONZE",
            "SUDESTE"
        );

        ErroCheckoutException erro = assertThrows(
            ErroCheckoutException.class,
            () -> service.calcularResumo(pedido)
        );
        assertEquals("PEDIDO_INVALIDO", erro.getCodigoErro());
    }

    @Test
    void erroItemPrecoZero() {
        PedidoRequest pedido = new PedidoRequest(
            List.of(new ItemCarrinho("Teste", BigDecimal.ZERO, 1, new BigDecimal("1.0"))),
            "ECONOMICA",
            null,
            "PIX",
            1,
            "BRONZE",
            "SUDESTE"
        );

        ErroCheckoutException erro = assertThrows(
            ErroCheckoutException.class,
            () -> service.calcularResumo(pedido)
        );
        assertEquals("PEDIDO_INVALIDO", erro.getCodigoErro());
    }

    @Test
    void erroNivelClubeInvalido() {
        PedidoRequest pedido = new PedidoRequest(
            List.of(new ItemCarrinho("Teste", new BigDecimal("10.00"), 1, new BigDecimal("1.0"))),
            "ECONOMICA",
            null,
            "PIX",
            1,
            "DIAMANTE",
            "SUDESTE"
        );

        ErroCheckoutException erro = assertThrows(
            ErroCheckoutException.class,
            () -> service.calcularResumo(pedido)
        );
        assertEquals("NIVEL_CLUBE_INVALIDO", erro.getCodigoErro());
    }

    @Test
    void erroRegiaoInvalida() {
        PedidoRequest pedido = new PedidoRequest(
            List.of(new ItemCarrinho("Teste", new BigDecimal("10.00"), 1, new BigDecimal("1.0"))),
            "ECONOMICA",
            null,
            "PIX",
            1,
            "BRONZE",
            "SUDOESTE"
        );

        ErroCheckoutException erro = assertThrows(
            ErroCheckoutException.class,
            () -> service.calcularResumo(pedido)
        );
        assertEquals("REGIAO_INVALIDA", erro.getCodigoErro());
    }

    @Test
    void erroModalidadeInvalida() {
        PedidoRequest pedido = new PedidoRequest(
            List.of(new ItemCarrinho("Teste", new BigDecimal("10.00"), 1, new BigDecimal("1.0"))),
            "DRONE",
            null,
            "PIX",
            1,
            "BRONZE",
            "SUDESTE"
        );

        ErroCheckoutException erro = assertThrows(
            ErroCheckoutException.class,
            () -> service.calcularResumo(pedido)
        );
        assertEquals("MODALIDADE_INVALIDA", erro.getCodigoErro());
    }

    @Test
    void erroModalidadeIndisponivel() {
        PedidoRequest pedido = new PedidoRequest(
            List.of(new ItemCarrinho("Teste", new BigDecimal("10.00"), 1, new BigDecimal("6.0"))),
            "MOTOBOY",
            null,
            "PIX",
            1,
            "BRONZE",
            "SUDESTE"
        );

        ErroCheckoutException erro = assertThrows(
            ErroCheckoutException.class,
            () -> service.calcularResumo(pedido)
        );
        assertEquals("MODALIDADE_INDISPONIVEL", erro.getCodigoErro());
    }

    @Test
    void erroCupomInvalido() {
        PedidoRequest pedido = new PedidoRequest(
            List.of(new ItemCarrinho("Teste", new BigDecimal("100.00"), 1, new BigDecimal("1.0"))),
            "ECONOMICA",
            "DESCONTO50",
            "PIX",
            1,
            "BRONZE",
            "SUDESTE"
        );

        ErroCheckoutException erro = assertThrows(
            ErroCheckoutException.class,
            () -> service.calcularResumo(pedido)
        );
        assertEquals("CUPOM_INVALIDO", erro.getCodigoErro());
    }

    @Test
    void erroCupomNaoAplicavel() {
        PedidoRequest pedido = new PedidoRequest(
            List.of(new ItemCarrinho("Teste", new BigDecimal("100.00"), 1, new BigDecimal("1.0"))),
            "ECONOMICA",
            "MENOS50",
            "PIX",
            1,
            "BRONZE",
            "SUDESTE"
        );

        ErroCheckoutException erro = assertThrows(
            ErroCheckoutException.class,
            () -> service.calcularResumo(pedido)
        );
        assertEquals("CUPOM_NAO_APLICAVEL", erro.getCodigoErro());
    }

    @Test
    void erroFormaPagamentoInvalida() {
        PedidoRequest pedido = new PedidoRequest(
            List.of(new ItemCarrinho("Teste", new BigDecimal("100.00"), 1, new BigDecimal("1.0"))),
            "ECONOMICA",
            null,
            "DINHEIRO",
            1,
            "BRONZE",
            "SUDESTE"
        );

        ErroCheckoutException erro = assertThrows(
            ErroCheckoutException.class,
            () -> service.calcularResumo(pedido)
        );
        assertEquals("FORMA_PAGAMENTO_INVALIDA", erro.getCodigoErro());
    }

    @Test
    void erroParcelamentoInvalido() {
        PedidoRequest pedido = new PedidoRequest(
            List.of(new ItemCarrinho("Teste", new BigDecimal("100.00"), 1, new BigDecimal("1.0"))),
            "ECONOMICA",
            null,
            "PIX",
            2,
            "BRONZE",
            "SUDESTE"
        );

        ErroCheckoutException erro = assertThrows(
            ErroCheckoutException.class,
            () -> service.calcularResumo(pedido)
        );
        assertEquals("PARCELAMENTO_INVALIDO", erro.getCodigoErro());
    }

    @Test
    void erroFormaPagamentoIndisponivel() {
        PedidoRequest pedido = new PedidoRequest(
            List.of(new ItemCarrinho("Teste", new BigDecimal("1500.00"), 1, new BigDecimal("1.0"))),
            "ECONOMICA",
            null,
            "BOLETO",
            1,
            "BRONZE",
            "SUDESTE"
        );

        ErroCheckoutException erro = assertThrows(
            ErroCheckoutException.class,
            () -> service.calcularResumo(pedido)
        );
        assertEquals("FORMA_PAGAMENTO_INDISPONIVEL", erro.getCodigoErro());
    }
}
