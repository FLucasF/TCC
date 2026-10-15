package com.loja.pedidos.service;

import com.loja.pedidos.model.Acao;
import com.loja.pedidos.model.Pedido;
import com.loja.pedidos.model.Situacao;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class PedidoService {
    private final Map<String, Pedido> pedidos = new HashMap<>();

    public Pedido criarPedido(BigDecimal valorProdutos, BigDecimal frete) {
        String id = UUID.randomUUID().toString();
        Pedido pedido = new Pedido(id, valorProdutos, frete);
        pedidos.put(id, pedido);
        return pedido;
    }

    public Pedido obterPedido(String id) {
        return pedidos.get(id);
    }

    public Pedido executarAcao(String id, Acao acao) {
        Pedido pedido = pedidos.get(id);
        if (pedido == null) {
            return null;
        }

        Situacao situacaoAtual = pedido.getSituacao();
        Situacao novaSituacao = null;

        switch (situacaoAtual) {
            case AGUARDANDO_PAGAMENTO:
                if (acao == Acao.PAGAR) {
                    novaSituacao = Situacao.PAGO;
                } else if (acao == Acao.CANCELAR) {
                    novaSituacao = Situacao.CANCELADO;
                    pedido.setValorReembolsado(BigDecimal.ZERO);
                }
                break;

            case PAGO:
                if (acao == Acao.SEPARAR) {
                    novaSituacao = Situacao.EM_SEPARACAO;
                } else if (acao == Acao.CANCELAR) {
                    novaSituacao = Situacao.CANCELADO;
                    pedido.setValorReembolsado(pedido.getValorTotal());
                }
                break;

            case EM_SEPARACAO:
                if (acao == Acao.ENVIAR) {
                    novaSituacao = Situacao.ENVIADO;
                } else if (acao == Acao.CANCELAR) {
                    novaSituacao = Situacao.CANCELADO;
                    BigDecimal taxa = new BigDecimal("15.00");
                    BigDecimal reembolso = pedido.getValorTotal().subtract(taxa);
                    if (reembolso.compareTo(BigDecimal.ZERO) < 0) {
                        reembolso = BigDecimal.ZERO;
                    }
                    pedido.setValorReembolsado(reembolso);
                    pedido.setEstoqueDevolvido(true);
                }
                break;

            case ENVIADO:
                if (acao == Acao.ENTREGAR) {
                    novaSituacao = Situacao.ENTREGUE;
                }
                break;

            case ENTREGUE:
                if (acao == Acao.DEVOLVER) {
                    novaSituacao = Situacao.DEVOLVIDO;
                    pedido.setValorReembolsado(pedido.getValorProdutos());
                    pedido.setColetaAgendada(true);
                }
                break;

            case CANCELADO:
            case DEVOLVIDO:
                break;
        }

        if (novaSituacao != null) {
            pedido.setSituacao(novaSituacao);
        }

        return pedido;
    }
}
