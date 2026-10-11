package com.loja.checkout;

import com.loja.checkout.dto.ItemRequest;
import com.loja.checkout.dto.PedidoRequest;
import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.exception.CodigoErro;
import com.loja.checkout.service.CalculadoraResumo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ValidacoesTests {

    @Autowired
    private CalculadoraResumo calculadora;

    @Test
    void deveLancarErroPedidoInvalidoQuandoCarrinhoVazio() {
        var request = new PedidoRequest(
            Collections.emptyList(), "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE"
        );

        CheckoutException ex = assertThrows(CheckoutException.class,
            () -> calculadora.calcular(request));
        assertEquals(CodigoErro.PEDIDO_INVALIDO, ex.getCodigo());
    }

    @Test
    void deveLancarErroPedidoInvalidoQuandoItemComPrecoZero() {
        var itens = List.of(
            new ItemRequest("Produto", BigDecimal.ZERO, 1, new BigDecimal("1.0"))
        );

        var request = new PedidoRequest(
            itens, "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE"
        );

        CheckoutException ex = assertThrows(CheckoutException.class,
            () -> calculadora.calcular(request));
        assertEquals(CodigoErro.PEDIDO_INVALIDO, ex.getCodigo());
    }

    @Test
    void deveLancarErroNivelClubeInvalido() {
        var itens = List.of(
            new ItemRequest("Produto", new BigDecimal("100.00"), 1, new BigDecimal("1.0"))
        );

        var request = new PedidoRequest(
            itens, "EXPRESSA", null, "PIX", 1, "DIAMANTE", "SUDESTE"
        );

        CheckoutException ex = assertThrows(CheckoutException.class,
            () -> calculadora.calcular(request));
        assertEquals(CodigoErro.NIVEL_CLUBE_INVALIDO, ex.getCodigo());
    }

    @Test
    void deveLancarErroRegiaoInvalida() {
        var itens = List.of(
            new ItemRequest("Produto", new BigDecimal("100.00"), 1, new BigDecimal("1.0"))
        );

        var request = new PedidoRequest(
            itens, "EXPRESSA", null, "PIX", 1, "BRONZE", "EXTERIOR"
        );

        CheckoutException ex = assertThrows(CheckoutException.class,
            () -> calculadora.calcular(request));
        assertEquals(CodigoErro.REGIAO_INVALIDA, ex.getCodigo());
    }

    @Test
    void deveLancarErroModalidadeInvalida() {
        var itens = List.of(
            new ItemRequest("Produto", new BigDecimal("100.00"), 1, new BigDecimal("1.0"))
        );

        var request = new PedidoRequest(
            itens, "DRONE", null, "PIX", 1, "BRONZE", "SUDESTE"
        );

        CheckoutException ex = assertThrows(CheckoutException.class,
            () -> calculadora.calcular(request));
        assertEquals(CodigoErro.MODALIDADE_INVALIDA, ex.getCodigo());
    }

    @Test
    void deveLancarErroModalidadeIndisponivelQuandoMotoboyAcimaDe5Kg() {
        var itens = List.of(
            new ItemRequest("Produto Pesado", new BigDecimal("100.00"), 1, new BigDecimal("6.0"))
        );

        var request = new PedidoRequest(
            itens, "MOTOBOY", null, "PIX", 1, "BRONZE", "SUDESTE"
        );

        CheckoutException ex = assertThrows(CheckoutException.class,
            () -> calculadora.calcular(request));
        assertEquals(CodigoErro.MODALIDADE_INDISPONIVEL, ex.getCodigo());
    }

    @Test
    void deveLancarErroCupomInvalido() {
        var itens = List.of(
            new ItemRequest("Produto", new BigDecimal("100.00"), 1, new BigDecimal("1.0"))
        );

        var request = new PedidoRequest(
            itens, "EXPRESSA", "CUPOM_INVALIDO", "PIX", 1, "BRONZE", "SUDESTE"
        );

        CheckoutException ex = assertThrows(CheckoutException.class,
            () -> calculadora.calcular(request));
        assertEquals(CodigoErro.CUPOM_INVALIDO, ex.getCodigo());
    }

    @Test
    void deveLancarErroCupomNaoAplicavelQuandoMenos50AbaixoDoMinimo() {
        var itens = List.of(
            new ItemRequest("Produto", new BigDecimal("100.00"), 1, new BigDecimal("1.0"))
        );

        var request = new PedidoRequest(
            itens, "EXPRESSA", "MENOS50", "PIX", 1, "BRONZE", "SUDESTE"
        );

        CheckoutException ex = assertThrows(CheckoutException.class,
            () -> calculadora.calcular(request));
        assertEquals(CodigoErro.CUPOM_NAO_APLICAVEL, ex.getCodigo());
    }

    @Test
    void deveLancarErroFormaPagamentoInvalida() {
        var itens = List.of(
            new ItemRequest("Produto", new BigDecimal("100.00"), 1, new BigDecimal("1.0"))
        );

        var request = new PedidoRequest(
            itens, "EXPRESSA", null, "BITCOIN", 1, "BRONZE", "SUDESTE"
        );

        CheckoutException ex = assertThrows(CheckoutException.class,
            () -> calculadora.calcular(request));
        assertEquals(CodigoErro.FORMA_PAGAMENTO_INVALIDA, ex.getCodigo());
    }

    @Test
    void deveLancarErroParcelamentoInvalidoQuandoPixComMaisDeUmaParcela() {
        var itens = List.of(
            new ItemRequest("Produto", new BigDecimal("100.00"), 1, new BigDecimal("1.0"))
        );

        var request = new PedidoRequest(
            itens, "EXPRESSA", null, "PIX", 2, "BRONZE", "SUDESTE"
        );

        CheckoutException ex = assertThrows(CheckoutException.class,
            () -> calculadora.calcular(request));
        assertEquals(CodigoErro.PARCELAMENTO_INVALIDO, ex.getCodigo());
    }

    @Test
    void deveLancarErroFormaPagamentoIndisponivelQuandoBoletoAcimaDe1000() {
        var itens = List.of(
            new ItemRequest("Produto Caro", new BigDecimal("1200.00"), 1, new BigDecimal("1.0"))
        );

        var request = new PedidoRequest(
            itens, "RETIRADA_LOJA", null, "BOLETO", 1, "BRONZE", "SUDESTE"
        );

        CheckoutException ex = assertThrows(CheckoutException.class,
            () -> calculadora.calcular(request));
        assertEquals(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL, ex.getCodigo());
    }
}
