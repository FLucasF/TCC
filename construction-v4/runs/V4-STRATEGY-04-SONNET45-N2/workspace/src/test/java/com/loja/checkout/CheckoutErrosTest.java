package com.loja.checkout;

import com.loja.checkout.dto.ItemCarrinho;
import com.loja.checkout.dto.PedidoRequest;
import com.loja.checkout.service.CheckoutException;
import com.loja.checkout.service.CheckoutService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CheckoutErrosTest {

    private final CheckoutService service = new CheckoutService();

    @Test
    void pedidoInvalido_carrinhoVazio() {
        PedidoRequest pedido = new PedidoRequest(
            List.of(),
            "EXPRESSA",
            null,
            "PIX",
            1,
            "BRONZE",
            "SUDESTE"
        );

        CheckoutException exception = assertThrows(CheckoutException.class,
            () -> service.calcularResumo(pedido));
        assertEquals("PEDIDO_INVALIDO", exception.getCodigo());
    }

    @Test
    void pedidoInvalido_precoZero() {
        List<ItemCarrinho> itens = List.of(
            new ItemCarrinho("Produto", new BigDecimal("0"), 1, new BigDecimal("0.5"))
        );

        PedidoRequest pedido = new PedidoRequest(
            itens,
            "EXPRESSA",
            null,
            "PIX",
            1,
            "BRONZE",
            "SUDESTE"
        );

        CheckoutException exception = assertThrows(CheckoutException.class,
            () -> service.calcularResumo(pedido));
        assertEquals("PEDIDO_INVALIDO", exception.getCodigo());
    }

    @Test
    void nivelClubeInvalido() {
        List<ItemCarrinho> itens = List.of(
            new ItemCarrinho("Produto", new BigDecimal("100"), 1, new BigDecimal("0.5"))
        );

        PedidoRequest pedido = new PedidoRequest(
            itens,
            "EXPRESSA",
            null,
            "PIX",
            1,
            "PLATINA",
            "SUDESTE"
        );

        CheckoutException exception = assertThrows(CheckoutException.class,
            () -> service.calcularResumo(pedido));
        assertEquals("NIVEL_CLUBE_INVALIDO", exception.getCodigo());
    }

    @Test
    void regiaoInvalida() {
        List<ItemCarrinho> itens = List.of(
            new ItemCarrinho("Produto", new BigDecimal("100"), 1, new BigDecimal("0.5"))
        );

        PedidoRequest pedido = new PedidoRequest(
            itens,
            "EXPRESSA",
            null,
            "PIX",
            1,
            "BRONZE",
            "EXTERIOR"
        );

        CheckoutException exception = assertThrows(CheckoutException.class,
            () -> service.calcularResumo(pedido));
        assertEquals("REGIAO_INVALIDA", exception.getCodigo());
    }

    @Test
    void modalidadeInvalida() {
        List<ItemCarrinho> itens = List.of(
            new ItemCarrinho("Produto", new BigDecimal("100"), 1, new BigDecimal("0.5"))
        );

        PedidoRequest pedido = new PedidoRequest(
            itens,
            "DRONE",
            null,
            "PIX",
            1,
            "BRONZE",
            "SUDESTE"
        );

        CheckoutException exception = assertThrows(CheckoutException.class,
            () -> service.calcularResumo(pedido));
        assertEquals("MODALIDADE_INVALIDA", exception.getCodigo());
    }

    @Test
    void modalidadeIndisponivel_motoboyAcima5kg() {
        List<ItemCarrinho> itens = List.of(
            new ItemCarrinho("Produto Pesado", new BigDecimal("100"), 1, new BigDecimal("6"))
        );

        PedidoRequest pedido = new PedidoRequest(
            itens,
            "MOTOBOY",
            null,
            "PIX",
            1,
            "BRONZE",
            "SUDESTE"
        );

        CheckoutException exception = assertThrows(CheckoutException.class,
            () -> service.calcularResumo(pedido));
        assertEquals("MODALIDADE_INDISPONIVEL", exception.getCodigo());
    }

    @Test
    void cupomInvalido() {
        List<ItemCarrinho> itens = List.of(
            new ItemCarrinho("Produto", new BigDecimal("100"), 1, new BigDecimal("0.5"))
        );

        PedidoRequest pedido = new PedidoRequest(
            itens,
            "EXPRESSA",
            "CUPOMINEXISTENTE",
            "PIX",
            1,
            "BRONZE",
            "SUDESTE"
        );

        CheckoutException exception = assertThrows(CheckoutException.class,
            () -> service.calcularResumo(pedido));
        assertEquals("CUPOM_INVALIDO", exception.getCodigo());
    }

    @Test
    void cupomNaoAplicavel_menos50Abaixo300() {
        List<ItemCarrinho> itens = List.of(
            new ItemCarrinho("Produto", new BigDecimal("100"), 1, new BigDecimal("0.5"))
        );

        PedidoRequest pedido = new PedidoRequest(
            itens,
            "EXPRESSA",
            "MENOS50",
            "PIX",
            1,
            "BRONZE",
            "SUDESTE"
        );

        CheckoutException exception = assertThrows(CheckoutException.class,
            () -> service.calcularResumo(pedido));
        assertEquals("CUPOM_NAO_APLICAVEL", exception.getCodigo());
    }

    @Test
    void formaPagamentoInvalida() {
        List<ItemCarrinho> itens = List.of(
            new ItemCarrinho("Produto", new BigDecimal("100"), 1, new BigDecimal("0.5"))
        );

        PedidoRequest pedido = new PedidoRequest(
            itens,
            "EXPRESSA",
            null,
            "CRIPTO",
            1,
            "BRONZE",
            "SUDESTE"
        );

        CheckoutException exception = assertThrows(CheckoutException.class,
            () -> service.calcularResumo(pedido));
        assertEquals("FORMA_PAGAMENTO_INVALIDA", exception.getCodigo());
    }

    @Test
    void parcelamentoInvalido_pixEmMaisDeUmaParcela() {
        List<ItemCarrinho> itens = List.of(
            new ItemCarrinho("Produto", new BigDecimal("100"), 1, new BigDecimal("0.5"))
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

        CheckoutException exception = assertThrows(CheckoutException.class,
            () -> service.calcularResumo(pedido));
        assertEquals("PARCELAMENTO_INVALIDO", exception.getCodigo());
    }

    @Test
    void parcelamentoInvalido_cartaoAcimaDe12x() {
        List<ItemCarrinho> itens = List.of(
            new ItemCarrinho("Produto", new BigDecimal("100"), 1, new BigDecimal("0.5"))
        );

        PedidoRequest pedido = new PedidoRequest(
            itens,
            "EXPRESSA",
            null,
            "CARTAO",
            15,
            "BRONZE",
            "SUDESTE"
        );

        CheckoutException exception = assertThrows(CheckoutException.class,
            () -> service.calcularResumo(pedido));
        assertEquals("PARCELAMENTO_INVALIDO", exception.getCodigo());
    }

    @Test
    void formaPagamentoIndisponivel_boletoAcima1000() {
        List<ItemCarrinho> itens = List.of(
            new ItemCarrinho("Produto Caro", new BigDecimal("1100"), 1, new BigDecimal("0.5"))
        );

        PedidoRequest pedido = new PedidoRequest(
            itens,
            "EXPRESSA",
            null,
            "BOLETO",
            1,
            "BRONZE",
            "SUDESTE"
        );

        CheckoutException exception = assertThrows(CheckoutException.class,
            () -> service.calcularResumo(pedido));
        assertEquals("FORMA_PAGAMENTO_INDISPONIVEL", exception.getCodigo());
    }
}
