package com.loja.pedidos.servico;

import com.loja.pedidos.dominio.Acao;
import com.loja.pedidos.dominio.Pedido;
import com.loja.pedidos.dominio.Situacao;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Tabela de transições: para cada situação, quais ações valem e o que cada uma faz.
 * Alterar o processo (nova etapa, nova regra) significa mexer só aqui.
 */
class Transicoes {

    private static final BigDecimal TAXA_SEPARACAO = new BigDecimal("15.00");

    private final Map<Situacao, Map<Acao, Consumer<Pedido>>> tabela = new EnumMap<>(Situacao.class);

    Transicoes() {
        registrar(Situacao.AGUARDANDO_PAGAMENTO, Acao.PAGAR, pedido ->
                pedido.setSituacao(Situacao.PAGO));

        registrar(Situacao.AGUARDANDO_PAGAMENTO, Acao.CANCELAR, pedido -> {
            pedido.setValorReembolsado(BigDecimal.ZERO);
            pedido.setSituacao(Situacao.CANCELADO);
        });

        registrar(Situacao.PAGO, Acao.SEPARAR, pedido ->
                pedido.setSituacao(Situacao.EM_SEPARACAO));

        registrar(Situacao.PAGO, Acao.CANCELAR, pedido -> {
            pedido.setValorReembolsado(pedido.getValorTotal());
            pedido.setSituacao(Situacao.CANCELADO);
        });

        registrar(Situacao.EM_SEPARACAO, Acao.ENVIAR, pedido ->
                pedido.setSituacao(Situacao.ENVIADO));

        registrar(Situacao.EM_SEPARACAO, Acao.CANCELAR, pedido -> {
            BigDecimal reembolso = pedido.getValorTotal().subtract(TAXA_SEPARACAO);
            if (reembolso.signum() < 0) {
                reembolso = BigDecimal.ZERO;
            }
            pedido.setValorReembolsado(reembolso);
            pedido.setEstoqueDevolvido(true);
            pedido.setSituacao(Situacao.CANCELADO);
        });

        registrar(Situacao.ENVIADO, Acao.ENTREGAR, pedido ->
                pedido.setSituacao(Situacao.ENTREGUE));

        registrar(Situacao.ENTREGUE, Acao.DEVOLVER, pedido -> {
            pedido.setValorReembolsado(pedido.getValorProdutos());
            pedido.setColetaAgendada(true);
            pedido.setSituacao(Situacao.DEVOLVIDO);
        });
    }

    private void registrar(Situacao situacao, Acao acao, Consumer<Pedido> efeito) {
        tabela.computeIfAbsent(situacao, s -> new EnumMap<>(Acao.class)).put(acao, efeito);
    }

    Consumer<Pedido> buscar(Situacao situacao, Acao acao) {
        return tabela.getOrDefault(situacao, Map.of()).get(acao);
    }
}
