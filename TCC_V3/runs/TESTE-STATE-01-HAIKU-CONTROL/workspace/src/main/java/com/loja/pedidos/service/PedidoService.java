package com.loja.pedidos.service;

import com.loja.pedidos.exception.PedidoException;
import com.loja.pedidos.model.Acao;
import com.loja.pedidos.model.Pedido;
import com.loja.pedidos.model.Situacao;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
public class PedidoService {
    private static final BigDecimal TAXA_SEPARACAO = new BigDecimal("15.00");
    private final Map<String, Pedido> pedidos = new HashMap<>();

    public Pedido criarPedido(BigDecimal valorProdutos, BigDecimal frete) throws PedidoException {
        if (valorProdutos == null || valorProdutos.compareTo(BigDecimal.ZERO) <= 0) {
            throw new PedidoException("PEDIDO_INVALIDO", 400);
        }
        if (frete == null || frete.compareTo(BigDecimal.ZERO) < 0) {
            throw new PedidoException("PEDIDO_INVALIDO", 400);
        }

        Pedido pedido = new Pedido(valorProdutos, frete);
        pedidos.put(pedido.getId(), pedido);
        return pedido;
    }

    public Pedido obterPedido(String id) throws PedidoException {
        Pedido pedido = pedidos.get(id);
        if (pedido == null) {
            throw new PedidoException("PEDIDO_NAO_ENCONTRADO", 404);
        }
        return pedido;
    }

    public Pedido executarAcao(String id, String acaoStr) throws PedidoException {
        Pedido pedido = obterPedido(id);

        if (acaoStr == null || acaoStr.isEmpty()) {
            throw new PedidoException("ACAO_INVALIDA", 400);
        }

        Acao acao;
        try {
            acao = Acao.valueOf(acaoStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new PedidoException("ACAO_INVALIDA", 400);
        }

        if (!ehAcaoPermitida(pedido.getSituacao(), acao)) {
            throw new PedidoException("ACAO_NAO_PERMITIDA", 409);
        }

        aplicarAcao(pedido, acao);
        return pedido;
    }

    private boolean ehAcaoPermitida(Situacao situacao, Acao acao) {
        return switch (situacao) {
            case AGUARDANDO_PAGAMENTO -> acao == Acao.PAGAR || acao == Acao.CANCELAR;
            case PAGO -> acao == Acao.SEPARAR || acao == Acao.CANCELAR;
            case EM_SEPARACAO -> acao == Acao.ENVIAR || acao == Acao.CANCELAR;
            case ENVIADO -> acao == Acao.ENTREGAR;
            case ENTREGUE -> acao == Acao.DEVOLVER;
            case CANCELADO, DEVOLVIDO -> false;
        };
    }

    private void aplicarAcao(Pedido pedido, Acao acao) {
        switch (pedido.getSituacao()) {
            case AGUARDANDO_PAGAMENTO:
                if (acao == Acao.PAGAR) {
                    pedido.setSituacao(Situacao.PAGO);
                } else if (acao == Acao.CANCELAR) {
                    pedido.setSituacao(Situacao.CANCELADO);
                }
                break;

            case PAGO:
                if (acao == Acao.SEPARAR) {
                    pedido.setSituacao(Situacao.EM_SEPARACAO);
                } else if (acao == Acao.CANCELAR) {
                    pedido.setSituacao(Situacao.CANCELADO);
                    pedido.setValorReembolsado(pedido.getValorTotal());
                }
                break;

            case EM_SEPARACAO:
                if (acao == Acao.ENVIAR) {
                    pedido.setSituacao(Situacao.ENVIADO);
                } else if (acao == Acao.CANCELAR) {
                    pedido.setSituacao(Situacao.CANCELADO);
                    BigDecimal reembolso = pedido.getValorTotal().subtract(TAXA_SEPARACAO);
                    if (reembolso.compareTo(BigDecimal.ZERO) < 0) {
                        reembolso = BigDecimal.ZERO;
                    }
                    pedido.setValorReembolsado(reembolso);
                    pedido.setEstoqueDevolvido(true);
                }
                break;

            case ENVIADO:
                if (acao == Acao.ENTREGAR) {
                    pedido.setSituacao(Situacao.ENTREGUE);
                }
                break;

            case ENTREGUE:
                if (acao == Acao.DEVOLVER) {
                    pedido.setSituacao(Situacao.DEVOLVIDO);
                    pedido.setValorReembolsado(pedido.getValorProdutos());
                    pedido.setColetaAgendada(true);
                }
                break;

            case CANCELADO:
            case DEVOLVIDO:
                break;
        }
    }
}
