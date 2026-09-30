package com.loja.pedidos;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class PedidoServiceTest {

    private PedidoService service;

    @BeforeEach
    void setUp() {
        service = new PedidoService();
    }

    @Test
    void exemplo1_cancelamentoEmSeparacao() {
        Pedido pedido = service.criar(new BigDecimal("200.00"), new BigDecimal("20.00"));

        service.executarAcao(pedido.getId(), Acao.PAGAR);
        service.executarAcao(pedido.getId(), Acao.SEPARAR);
        service.executarAcao(pedido.getId(), Acao.CANCELAR);

        Pedido resultado = service.obter(pedido.getId());
        assertEquals(Situacao.CANCELADO, resultado.getSituacao());
        assertEquals(new BigDecimal("220.00"), resultado.getValorTotal());
        assertEquals(new BigDecimal("205.00"), resultado.getValorReembolsado());
        assertTrue(resultado.isEstoqueDevolvido());
        assertFalse(resultado.isColetaAgendada());
        assertEquals(4, resultado.getHistorico().size());
    }

    @Test
    void exemplo2_devolucaoCompleta() {
        Pedido pedido = service.criar(new BigDecimal("150.00"), new BigDecimal("12.50"));

        service.executarAcao(pedido.getId(), Acao.PAGAR);
        service.executarAcao(pedido.getId(), Acao.SEPARAR);
        service.executarAcao(pedido.getId(), Acao.ENVIAR);
        service.executarAcao(pedido.getId(), Acao.ENTREGAR);
        service.executarAcao(pedido.getId(), Acao.DEVOLVER);

        Pedido resultado = service.obter(pedido.getId());
        assertEquals(Situacao.DEVOLVIDO, resultado.getSituacao());
        assertEquals(new BigDecimal("162.50"), resultado.getValorTotal());
        assertEquals(new BigDecimal("150.00"), resultado.getValorReembolsado());
        assertFalse(resultado.isEstoqueDevolvido());
        assertTrue(resultado.isColetaAgendada());
    }

    @Test
    void exemplo3_cancelamentoAntesDeStartar() {
        Pedido pedido = service.criar(new BigDecimal("80.00"), new BigDecimal("0.00"));

        service.executarAcao(pedido.getId(), Acao.CANCELAR);

        Pedido resultado = service.obter(pedido.getId());
        assertEquals(Situacao.CANCELADO, resultado.getSituacao());
        assertEquals(new BigDecimal("0.00"), resultado.getValorReembolsado());
        assertFalse(resultado.isEstoqueDevolvido());
        assertFalse(resultado.isColetaAgendada());
    }

    @Test
    void exemplo4_cancelamentoAposPagamento() {
        Pedido pedido = service.criar(new BigDecimal("250.00"), new BigDecimal("25.00"));

        service.executarAcao(pedido.getId(), Acao.PAGAR);
        service.executarAcao(pedido.getId(), Acao.CANCELAR);

        Pedido resultado = service.obter(pedido.getId());
        assertEquals(Situacao.CANCELADO, resultado.getSituacao());
        assertEquals(new BigDecimal("275.00"), resultado.getValorReembolsado());
    }

    @Test
    void exemplo5_taxaMaiorQuoTotal() {
        Pedido pedido = service.criar(new BigDecimal("10.00"), new BigDecimal("3.00"));

        service.executarAcao(pedido.getId(), Acao.PAGAR);
        service.executarAcao(pedido.getId(), Acao.SEPARAR);
        service.executarAcao(pedido.getId(), Acao.CANCELAR);

        Pedido resultado = service.obter(pedido.getId());
        assertEquals(Situacao.CANCELADO, resultado.getSituacao());
        assertEquals(new BigDecimal("13.00"), resultado.getValorTotal());
        assertEquals(new BigDecimal("0.00"), resultado.getValorReembolsado());
        assertTrue(resultado.isEstoqueDevolvido());
    }

    @Test
    void exemplo6_acaoNaoPermitida() {
        Pedido pedido = service.criar(new BigDecimal("99.90"), new BigDecimal("15.00"));

        service.executarAcao(pedido.getId(), Acao.PAGAR);
        Pedido resultado = service.executarAcao(pedido.getId(), Acao.ENVIAR);

        assertNull(resultado);
        Pedido pedidoConsultado = service.obter(pedido.getId());
        assertEquals(Situacao.PAGO, pedidoConsultado.getSituacao());
    }

    @Test
    void exemplo7_cancelamentoAposEnvio() {
        Pedido pedido = service.criar(new BigDecimal("100.00"), new BigDecimal("10.00"));

        service.executarAcao(pedido.getId(), Acao.PAGAR);
        service.executarAcao(pedido.getId(), Acao.SEPARAR);
        service.executarAcao(pedido.getId(), Acao.ENVIAR);
        Pedido resultado = service.executarAcao(pedido.getId(), Acao.CANCELAR);

        assertNull(resultado);
        Pedido pedidoConsultado = service.obter(pedido.getId());
        assertEquals(Situacao.ENVIADO, pedidoConsultado.getSituacao());
    }

    @Test
    void pedidoNaoEncontrado() {
        Pedido pedido = service.obter("id-inexistente");
        assertNull(pedido);
    }
}
