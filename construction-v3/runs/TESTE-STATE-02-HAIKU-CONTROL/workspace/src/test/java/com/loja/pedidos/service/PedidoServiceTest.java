package com.loja.pedidos.service;

import com.loja.pedidos.model.Acao;
import com.loja.pedidos.model.Pedido;
import com.loja.pedidos.model.Situacao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PedidoServiceTest {
    private PedidoService service;

    @BeforeEach
    void setUp() {
        service = new PedidoService();
    }

    @Test
    void exemplo1_cancelamento_em_separacao() {
        Pedido pedido = service.criarPedido(new BigDecimal("200.00"), new BigDecimal("20.00"));
        String id = pedido.getId();

        service.executarAcao(id, Acao.PAGAR);
        service.executarAcao(id, Acao.SEPARAR);
        service.executarAcao(id, Acao.CANCELAR);

        Pedido resultado = service.obterPedido(id);
        assertEquals(Situacao.CANCELADO, resultado.getSituacao());
        assertEquals(new BigDecimal("220.00"), resultado.getValorTotal());
        assertEquals(new BigDecimal("205.00"), resultado.getValorReembolsado());
        assertTrue(resultado.isEstoqueDevolvido());
        assertFalse(resultado.isColetaAgendada());
        assertEquals(
            List.of(Situacao.AGUARDANDO_PAGAMENTO, Situacao.PAGO, Situacao.EM_SEPARACAO, Situacao.CANCELADO),
            resultado.getHistorico()
        );
    }

    @Test
    void exemplo2_devolucao_completa() {
        Pedido pedido = service.criarPedido(new BigDecimal("150.00"), new BigDecimal("12.50"));
        String id = pedido.getId();

        service.executarAcao(id, Acao.PAGAR);
        service.executarAcao(id, Acao.SEPARAR);
        service.executarAcao(id, Acao.ENVIAR);
        service.executarAcao(id, Acao.ENTREGAR);
        service.executarAcao(id, Acao.DEVOLVER);

        Pedido resultado = service.obterPedido(id);
        assertEquals(Situacao.DEVOLVIDO, resultado.getSituacao());
        assertEquals(new BigDecimal("162.50"), resultado.getValorTotal());
        assertEquals(new BigDecimal("150.00"), resultado.getValorReembolsado());
        assertFalse(resultado.isEstoqueDevolvido());
        assertTrue(resultado.isColetaAgendada());
    }

    @Test
    void exemplo3_cancelamento_sem_pagamento() {
        Pedido pedido = service.criarPedido(new BigDecimal("80.00"), new BigDecimal("0.00"));
        String id = pedido.getId();

        service.executarAcao(id, Acao.CANCELAR);

        Pedido resultado = service.obterPedido(id);
        assertEquals(Situacao.CANCELADO, resultado.getSituacao());
        assertEquals(0, resultado.getValorReembolsado().compareTo(new BigDecimal("0.00")));
        assertFalse(resultado.isEstoqueDevolvido());
        assertFalse(resultado.isColetaAgendada());
    }

    @Test
    void exemplo4_cancelamento_apos_pagamento() {
        Pedido pedido = service.criarPedido(new BigDecimal("250.00"), new BigDecimal("25.00"));
        String id = pedido.getId();

        service.executarAcao(id, Acao.PAGAR);
        service.executarAcao(id, Acao.CANCELAR);

        Pedido resultado = service.obterPedido(id);
        assertEquals(Situacao.CANCELADO, resultado.getSituacao());
        assertEquals(new BigDecimal("275.00"), resultado.getValorReembolsado());
    }

    @Test
    void exemplo5_cancelamento_em_separacao_taxa_maior_que_total() {
        Pedido pedido = service.criarPedido(new BigDecimal("10.00"), new BigDecimal("3.00"));
        String id = pedido.getId();

        service.executarAcao(id, Acao.PAGAR);
        service.executarAcao(id, Acao.SEPARAR);
        service.executarAcao(id, Acao.CANCELAR);

        Pedido resultado = service.obterPedido(id);
        assertEquals(Situacao.CANCELADO, resultado.getSituacao());
        assertEquals(new BigDecimal("13.00"), resultado.getValorTotal());
        assertEquals(0, resultado.getValorReembolsado().compareTo(new BigDecimal("0.00")));
        assertTrue(resultado.isEstoqueDevolvido());
    }

    @Test
    void exemplo6_envio_sem_separacao_nao_permitido() {
        Pedido pedido = service.criarPedido(new BigDecimal("99.90"), new BigDecimal("15.00"));
        String id = pedido.getId();

        service.executarAcao(id, Acao.PAGAR);
        Pedido antigo = service.obterPedido(id);
        service.executarAcao(id, Acao.ENVIAR);
        Pedido resultado = service.obterPedido(id);

        assertEquals(Situacao.PAGO, resultado.getSituacao());
        assertEquals(Situacao.PAGO, antigo.getSituacao());
    }

    @Test
    void exemplo7_cancelamento_pedido_enviado_nao_permitido() {
        Pedido pedido = service.criarPedido(new BigDecimal("100.00"), new BigDecimal("10.00"));
        String id = pedido.getId();

        service.executarAcao(id, Acao.PAGAR);
        service.executarAcao(id, Acao.SEPARAR);
        service.executarAcao(id, Acao.ENVIAR);
        Pedido antigo = service.obterPedido(id);
        service.executarAcao(id, Acao.CANCELAR);
        Pedido resultado = service.obterPedido(id);

        assertEquals(Situacao.ENVIADO, resultado.getSituacao());
        assertEquals(Situacao.ENVIADO, antigo.getSituacao());
    }

    @Test
    void fluxo_completo_sem_problemas() {
        Pedido pedido = service.criarPedido(new BigDecimal("100.00"), new BigDecimal("10.00"));
        String id = pedido.getId();

        assertEquals(Situacao.AGUARDANDO_PAGAMENTO, pedido.getSituacao());

        service.executarAcao(id, Acao.PAGAR);
        Pedido resultado = service.obterPedido(id);
        assertEquals(Situacao.PAGO, resultado.getSituacao());

        service.executarAcao(id, Acao.SEPARAR);
        resultado = service.obterPedido(id);
        assertEquals(Situacao.EM_SEPARACAO, resultado.getSituacao());

        service.executarAcao(id, Acao.ENVIAR);
        resultado = service.obterPedido(id);
        assertEquals(Situacao.ENVIADO, resultado.getSituacao());

        service.executarAcao(id, Acao.ENTREGAR);
        resultado = service.obterPedido(id);
        assertEquals(Situacao.ENTREGUE, resultado.getSituacao());

        service.executarAcao(id, Acao.DEVOLVER);
        resultado = service.obterPedido(id);
        assertEquals(Situacao.DEVOLVIDO, resultado.getSituacao());
        assertEquals(new BigDecimal("100.00"), resultado.getValorReembolsado());
        assertTrue(resultado.isColetaAgendada());
    }

    @Test
    void fluxo_alternativo_cancelamento_pago() {
        Pedido pedido = service.criarPedido(new BigDecimal("100.00"), new BigDecimal("10.00"));
        String id = pedido.getId();

        service.executarAcao(id, Acao.PAGAR);
        service.executarAcao(id, Acao.CANCELAR);

        Pedido resultado = service.obterPedido(id);
        assertEquals(Situacao.CANCELADO, resultado.getSituacao());
        assertEquals(new BigDecimal("110.00"), resultado.getValorReembolsado());
    }

    @Test
    void historico_completo() {
        Pedido pedido = service.criarPedido(new BigDecimal("100.00"), new BigDecimal("10.00"));
        String id = pedido.getId();

        service.executarAcao(id, Acao.PAGAR);
        service.executarAcao(id, Acao.SEPARAR);

        Pedido resultado = service.obterPedido(id);
        List<Situacao> historico = resultado.getHistorico();

        assertEquals(3, historico.size());
        assertEquals(Situacao.AGUARDANDO_PAGAMENTO, historico.get(0));
        assertEquals(Situacao.PAGO, historico.get(1));
        assertEquals(Situacao.EM_SEPARACAO, historico.get(2));
    }

    @Test
    void reembolso_zero_quando_cancelado_sem_pagamento() {
        Pedido pedido = service.criarPedido(new BigDecimal("100.00"), new BigDecimal("10.00"));
        String id = pedido.getId();

        service.executarAcao(id, Acao.CANCELAR);

        Pedido resultado = service.obterPedido(id);
        assertEquals(BigDecimal.ZERO, resultado.getValorReembolsado());
    }

    @Test
    void estado_cancelado_nao_aceita_acoes() {
        Pedido pedido = service.criarPedido(new BigDecimal("100.00"), new BigDecimal("10.00"));
        String id = pedido.getId();

        service.executarAcao(id, Acao.CANCELAR);
        Pedido antigo = service.obterPedido(id);

        service.executarAcao(id, Acao.PAGAR);
        Pedido resultado = service.obterPedido(id);

        assertEquals(Situacao.CANCELADO, resultado.getSituacao());
        assertEquals(Situacao.CANCELADO, antigo.getSituacao());
    }

    @Test
    void estado_devolvido_nao_aceita_acoes() {
        Pedido pedido = service.criarPedido(new BigDecimal("100.00"), new BigDecimal("10.00"));
        String id = pedido.getId();

        service.executarAcao(id, Acao.PAGAR);
        service.executarAcao(id, Acao.SEPARAR);
        service.executarAcao(id, Acao.ENVIAR);
        service.executarAcao(id, Acao.ENTREGAR);
        service.executarAcao(id, Acao.DEVOLVER);
        Pedido antigo = service.obterPedido(id);

        service.executarAcao(id, Acao.PAGAR);
        Pedido resultado = service.obterPedido(id);

        assertEquals(Situacao.DEVOLVIDO, resultado.getSituacao());
        assertEquals(Situacao.DEVOLVIDO, antigo.getSituacao());
    }
}
