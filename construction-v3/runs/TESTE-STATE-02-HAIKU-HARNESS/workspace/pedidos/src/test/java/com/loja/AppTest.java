package com.loja;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class AppTest {
    private PedidoService pedidoService;

    @BeforeEach
    public void setup() {
        pedidoService = new PedidoService();
    }

    @Test
    public void exemplo1() {
        Pedido p = pedidoService.criarPedido(new BigDecimal("200.00"), new BigDecimal("20.00"));
        pedidoService.executarAcao(p.getId(), Acao.PAGAR);
        pedidoService.executarAcao(p.getId(), Acao.SEPARAR);
        pedidoService.executarAcao(p.getId(), Acao.CANCELAR);

        assertEquals(Situacao.CANCELADO, p.getSituacao());
        assertEquals(new BigDecimal("220.00"), p.getValorTotal());
        assertEquals(new BigDecimal("205.00"), p.getValorReembolsado());
        assertTrue(p.isEstoqueDevolvido());
        assertFalse(p.isColetaAgendada());
        assertEquals(List.of("AGUARDANDO_PAGAMENTO", "PAGO", "EM_SEPARACAO", "CANCELADO"),
            p.getHistorico().stream().map(Situacao::toString).toList());
    }

    @Test
    public void exemplo2() {
        Pedido p = pedidoService.criarPedido(new BigDecimal("150.00"), new BigDecimal("12.50"));
        pedidoService.executarAcao(p.getId(), Acao.PAGAR);
        pedidoService.executarAcao(p.getId(), Acao.SEPARAR);
        pedidoService.executarAcao(p.getId(), Acao.ENVIAR);
        pedidoService.executarAcao(p.getId(), Acao.ENTREGAR);
        pedidoService.executarAcao(p.getId(), Acao.DEVOLVER);

        assertEquals(Situacao.DEVOLVIDO, p.getSituacao());
        assertEquals(new BigDecimal("162.50"), p.getValorTotal());
        assertEquals(new BigDecimal("150.00"), p.getValorReembolsado());
        assertFalse(p.isEstoqueDevolvido());
        assertTrue(p.isColetaAgendada());
    }

    @Test
    public void exemplo3() {
        Pedido p = pedidoService.criarPedido(new BigDecimal("80.00"), new BigDecimal("0.00"));
        pedidoService.executarAcao(p.getId(), Acao.CANCELAR);

        assertEquals(Situacao.CANCELADO, p.getSituacao());
        assertEquals(0, p.getValorReembolsado().compareTo(new BigDecimal("0.00")));
        assertFalse(p.isEstoqueDevolvido());
        assertFalse(p.isColetaAgendada());
    }

    @Test
    public void exemplo4() {
        Pedido p = pedidoService.criarPedido(new BigDecimal("250.00"), new BigDecimal("25.00"));
        pedidoService.executarAcao(p.getId(), Acao.PAGAR);
        pedidoService.executarAcao(p.getId(), Acao.CANCELAR);

        assertEquals(Situacao.CANCELADO, p.getSituacao());
        assertEquals(new BigDecimal("275.00"), p.getValorReembolsado());
    }

    @Test
    public void exemplo5() {
        Pedido p = pedidoService.criarPedido(new BigDecimal("10.00"), new BigDecimal("3.00"));
        pedidoService.executarAcao(p.getId(), Acao.PAGAR);
        pedidoService.executarAcao(p.getId(), Acao.SEPARAR);
        pedidoService.executarAcao(p.getId(), Acao.CANCELAR);

        assertEquals(Situacao.CANCELADO, p.getSituacao());
        assertEquals(0, p.getValorTotal().compareTo(new BigDecimal("13.00")));
        assertEquals(0, p.getValorReembolsado().compareTo(new BigDecimal("0.00")));
        assertTrue(p.isEstoqueDevolvido());
        assertFalse(p.isColetaAgendada());
    }
}
