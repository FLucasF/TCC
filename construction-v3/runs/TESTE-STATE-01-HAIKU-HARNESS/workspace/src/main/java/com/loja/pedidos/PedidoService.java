package com.loja.pedidos;

import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
public class PedidoService {
    private final Map<String, Pedido> pedidos = new HashMap<>();

    public Pedido criar(BigDecimal valorProdutos, BigDecimal frete) {
        Pedido pedido = new Pedido(valorProdutos, frete);
        pedidos.put(pedido.getId(), pedido);
        return pedido;
    }

    public Pedido obter(String id) {
        return pedidos.get(id);
    }

    public Pedido executarAcao(String id, Acao acao) {
        Pedido pedido = pedidos.get(id);
        if (pedido == null) {
            return null;
        }

        switch (pedido.getSituacao()) {
            case AGUARDANDO_PAGAMENTO:
                if (acao == Acao.PAGAR) {
                    pedido.setSituacao(Situacao.PAGO);
                    return pedido;
                } else if (acao == Acao.CANCELAR) {
                    pedido.setSituacao(Situacao.CANCELADO);
                    return pedido;
                }
                break;

            case PAGO:
                if (acao == Acao.SEPARAR) {
                    pedido.setSituacao(Situacao.EM_SEPARACAO);
                    return pedido;
                } else if (acao == Acao.CANCELAR) {
                    pedido.setSituacao(Situacao.CANCELADO);
                    pedido.setValorReembolsado(pedido.getValorTotal());
                    return pedido;
                }
                break;

            case EM_SEPARACAO:
                if (acao == Acao.ENVIAR) {
                    pedido.setSituacao(Situacao.ENVIADO);
                    return pedido;
                } else if (acao == Acao.CANCELAR) {
                    pedido.setSituacao(Situacao.CANCELADO);
                    BigDecimal taxa = new BigDecimal("15.00");
                    BigDecimal reembolso = pedido.getValorTotal().subtract(taxa);
                    if (reembolso.compareTo(BigDecimal.ZERO) < 0) {
                        reembolso = BigDecimal.ZERO;
                    }
                    pedido.setValorReembolsado(reembolso);
                    pedido.setEstoqueDevolvido(true);
                    return pedido;
                }
                break;

            case ENVIADO:
                if (acao == Acao.ENTREGAR) {
                    pedido.setSituacao(Situacao.ENTREGUE);
                    return pedido;
                }
                break;

            case ENTREGUE:
                if (acao == Acao.DEVOLVER) {
                    pedido.setSituacao(Situacao.DEVOLVIDO);
                    pedido.setValorReembolsado(pedido.getValorProdutos());
                    pedido.setColetaAgendada(true);
                    return pedido;
                }
                break;

            case CANCELADO:
            case DEVOLVIDO:
                break;
        }

        return null;
    }
}
