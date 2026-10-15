package com.loja;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PedidoService {
    private Map<String, Pedido> pedidos = new HashMap<>();

    public Pedido criarPedido(BigDecimal valorProdutos, BigDecimal frete) {
        String id = UUID.randomUUID().toString();
        Pedido pedido = new Pedido(id, valorProdutos, frete);
        pedidos.put(id, pedido);
        return pedido;
    }

    public Pedido obterPedido(String id) {
        return pedidos.get(id);
    }

    public Pedido executarAcao(String pedidoId, Acao acao) {
        Pedido pedido = pedidos.get(pedidoId);
        if (pedido == null) {
            return null;
        }

        processarAcao(pedido, acao);
        return pedido;
    }

    private void processarAcao(Pedido pedido, Acao acao) {
        switch (pedido.getSituacao()) {
            case AGUARDANDO_PAGAMENTO -> processarAguardandoPagamento(pedido, acao);
            case PAGO -> processarPago(pedido, acao);
            case EM_SEPARACAO -> processarEmSeparacao(pedido, acao);
            case ENVIADO -> processarEnviado(pedido, acao);
            case ENTREGUE -> processarEntregue(pedido, acao);
            case CANCELADO, DEVOLVIDO -> {}
        }
    }

    private void processarAguardandoPagamento(Pedido pedido, Acao acao) {
        switch (acao) {
            case PAGAR:
                pedido.setSituacao(Situacao.PAGO);
                break;
            case CANCELAR:
                pedido.setSituacao(Situacao.CANCELADO);
                pedido.setValorReembolsado(BigDecimal.ZERO);
                break;
            default:
                break;
        }
    }

    private void processarPago(Pedido pedido, Acao acao) {
        switch (acao) {
            case SEPARAR:
                pedido.setSituacao(Situacao.EM_SEPARACAO);
                break;
            case CANCELAR:
                pedido.setSituacao(Situacao.CANCELADO);
                pedido.setValorReembolsado(pedido.getValorTotal());
                break;
            default:
                break;
        }
    }

    private void processarEmSeparacao(Pedido pedido, Acao acao) {
        switch (acao) {
            case ENVIAR:
                pedido.setSituacao(Situacao.ENVIADO);
                break;
            case CANCELAR:
                pedido.setSituacao(Situacao.CANCELADO);
                BigDecimal taxa = new BigDecimal("15.00");
                BigDecimal reembolso = pedido.getValorTotal().subtract(taxa);
                if (reembolso.compareTo(BigDecimal.ZERO) < 0) {
                    reembolso = BigDecimal.ZERO;
                }
                pedido.setValorReembolsado(reembolso);
                pedido.setEstoqueDevolvido(true);
                break;
            default:
                break;
        }
    }

    private void processarEnviado(Pedido pedido, Acao acao) {
        switch (acao) {
            case ENTREGAR:
                pedido.setSituacao(Situacao.ENTREGUE);
                break;
            default:
                break;
        }
    }

    private void processarEntregue(Pedido pedido, Acao acao) {
        switch (acao) {
            case DEVOLVER:
                pedido.setSituacao(Situacao.DEVOLVIDO);
                pedido.setValorReembolsado(pedido.getValorProdutos());
                pedido.setColetaAgendada(true);
                break;
            default:
                break;
        }
    }
}
