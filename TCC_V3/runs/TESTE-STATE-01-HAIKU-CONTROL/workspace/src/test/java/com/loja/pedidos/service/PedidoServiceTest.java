package com.loja.pedidos.service;

import com.loja.pedidos.exception.PedidoException;
import com.loja.pedidos.model.Pedido;
import com.loja.pedidos.model.Situacao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class PedidoServiceTest {
    private PedidoService pedidoService;

    @BeforeEach
    void setUp() {
        pedidoService = new PedidoService();
    }

    @Test
    void testCriarPedidoValido() throws PedidoException {
        Pedido pedido = pedidoService.criarPedido(new BigDecimal("200.00"), new BigDecimal("20.00"));
        assertNotNull(pedido.getId());
        assertEquals(Situacao.AGUARDANDO_PAGAMENTO, pedido.getSituacao());
        assertEquals(new BigDecimal("200.00"), pedido.getValorProdutos());
        assertEquals(new BigDecimal("20.00"), pedido.getFrete());
        assertEquals(new BigDecimal("220.00"), pedido.getValorTotal());
    }

    @Test
    void testCriarPedidoValorProdutosZero() {
        assertThrows(PedidoException.class, () ->
            pedidoService.criarPedido(BigDecimal.ZERO, new BigDecimal("20.00"))
        );
    }

    @Test
    void testCriarPedidoValorProdutosNegativo() {
        assertThrows(PedidoException.class, () ->
            pedidoService.criarPedido(new BigDecimal("-100.00"), new BigDecimal("20.00"))
        );
    }

    @Test
    void testCriarPedidoValorProdutosNulo() {
        assertThrows(PedidoException.class, () ->
            pedidoService.criarPedido(null, new BigDecimal("20.00"))
        );
    }

    @Test
    void testCriarPedidoFreteNegativo() {
        assertThrows(PedidoException.class, () ->
            pedidoService.criarPedido(new BigDecimal("200.00"), new BigDecimal("-10.00"))
        );
    }

    @Test
    void testCriarPedidoFreteNulo() {
        assertThrows(PedidoException.class, () ->
            pedidoService.criarPedido(new BigDecimal("200.00"), null)
        );
    }

    @Test
    void testObterPedidoNaoEncontrado() {
        assertThrows(PedidoException.class, () ->
            pedidoService.obterPedido("id-inexistente")
        );
    }

    @Test
    void testAcaoInvalida() throws PedidoException {
        Pedido pedido = pedidoService.criarPedido(new BigDecimal("200.00"), new BigDecimal("20.00"));
        assertThrows(PedidoException.class, () ->
            pedidoService.executarAcao(pedido.getId(), "TROCAR")
        );
    }

    @Test
    void testAcaoAusente() throws PedidoException {
        Pedido pedido = pedidoService.criarPedido(new BigDecimal("200.00"), new BigDecimal("20.00"));
        assertThrows(PedidoException.class, () ->
            pedidoService.executarAcao(pedido.getId(), null)
        );
    }

    @Test
    void testExemplo1() throws PedidoException {
        Pedido pedido = pedidoService.criarPedido(new BigDecimal("200.00"), new BigDecimal("20.00"));

        pedidoService.executarAcao(pedido.getId(), "PAGAR");
        assertEquals(Situacao.PAGO, pedido.getSituacao());

        pedidoService.executarAcao(pedido.getId(), "SEPARAR");
        assertEquals(Situacao.EM_SEPARACAO, pedido.getSituacao());

        pedidoService.executarAcao(pedido.getId(), "CANCELAR");
        assertEquals(Situacao.CANCELADO, pedido.getSituacao());
        assertEquals(new BigDecimal("220.00"), pedido.getValorTotal());
        assertEquals(new BigDecimal("205.00"), pedido.getValorReembolsado());
        assertTrue(pedido.isEstoqueDevolvido());
        assertFalse(pedido.isColetaAgendada());

        assertEquals(7, pedido.getHistorico().size());
    }

    @Test
    void testExemplo2() throws PedidoException {
        Pedido pedido = pedidoService.criarPedido(new BigDecimal("150.00"), new BigDecimal("12.50"));

        pedidoService.executarAcao(pedido.getId(), "PAGAR");
        pedidoService.executarAcao(pedido.getId(), "SEPARAR");
        pedidoService.executarAcao(pedido.getId(), "ENVIAR");
        pedidoService.executarAcao(pedido.getId(), "ENTREGAR");
        pedidoService.executarAcao(pedido.getId(), "DEVOLVER");

        assertEquals(Situacao.DEVOLVIDO, pedido.getSituacao());
        assertEquals(new BigDecimal("162.50"), pedido.getValorTotal());
        assertEquals(new BigDecimal("150.00"), pedido.getValorReembolsado());
        assertFalse(pedido.isEstoqueDevolvido());
        assertTrue(pedido.isColetaAgendada());
    }

    @Test
    void testExemplo3() throws PedidoException {
        Pedido pedido = pedidoService.criarPedido(new BigDecimal("80.00"), new BigDecimal("0.00"));

        pedidoService.executarAcao(pedido.getId(), "CANCELAR");

        assertEquals(Situacao.CANCELADO, pedido.getSituacao());
        assertEquals(new BigDecimal("0.00"), pedido.getValorReembolsado());
        assertFalse(pedido.isEstoqueDevolvido());
        assertFalse(pedido.isColetaAgendada());
    }

    @Test
    void testExemplo4() throws PedidoException {
        Pedido pedido = pedidoService.criarPedido(new BigDecimal("250.00"), new BigDecimal("25.00"));

        pedidoService.executarAcao(pedido.getId(), "PAGAR");
        pedidoService.executarAcao(pedido.getId(), "CANCELAR");

        assertEquals(Situacao.CANCELADO, pedido.getSituacao());
        assertEquals(new BigDecimal("275.00"), pedido.getValorReembolsado());
    }

    @Test
    void testExemplo5() throws PedidoException {
        Pedido pedido = pedidoService.criarPedido(new BigDecimal("10.00"), new BigDecimal("3.00"));

        pedidoService.executarAcao(pedido.getId(), "PAGAR");
        pedidoService.executarAcao(pedido.getId(), "SEPARAR");
        pedidoService.executarAcao(pedido.getId(), "CANCELAR");

        assertEquals(Situacao.CANCELADO, pedido.getSituacao());
        assertEquals(new BigDecimal("13.00"), pedido.getValorTotal());
        assertEquals(new BigDecimal("0.00"), pedido.getValorReembolsado());
        assertTrue(pedido.isEstoqueDevolvido());
    }

    @Test
    void testExemplo6() throws PedidoException {
        Pedido pedido = pedidoService.criarPedido(new BigDecimal("99.90"), new BigDecimal("15.00"));

        pedidoService.executarAcao(pedido.getId(), "PAGAR");

        PedidoException exception = assertThrows(PedidoException.class, () ->
            pedidoService.executarAcao(pedido.getId(), "ENVIAR")
        );
        assertEquals("ACAO_NAO_PERMITIDA", exception.getCodigo());
        assertEquals(409, exception.getStatusCode());
        assertEquals(Situacao.PAGO, pedido.getSituacao());
    }

    @Test
    void testExemplo7() throws PedidoException {
        Pedido pedido = pedidoService.criarPedido(new BigDecimal("100.00"), new BigDecimal("10.00"));

        pedidoService.executarAcao(pedido.getId(), "PAGAR");
        pedidoService.executarAcao(pedido.getId(), "SEPARAR");
        pedidoService.executarAcao(pedido.getId(), "ENVIAR");

        PedidoException exception = assertThrows(PedidoException.class, () ->
            pedidoService.executarAcao(pedido.getId(), "CANCELAR")
        );
        assertEquals("ACAO_NAO_PERMITIDA", exception.getCodigo());
        assertEquals(409, exception.getStatusCode());
        assertEquals(Situacao.ENVIADO, pedido.getSituacao());
    }

    @Test
    void testExemplo8() throws PedidoException {
        Pedido pedido = pedidoService.criarPedido(new BigDecimal("100.00"), new BigDecimal("10.00"));

        PedidoException exception = assertThrows(PedidoException.class, () ->
            pedidoService.executarAcao(pedido.getId(), "TROCAR")
        );
        assertEquals("ACAO_INVALIDA", exception.getCodigo());
        assertEquals(400, exception.getStatusCode());
    }
}
