package com.loja.pedidos;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Service;

@Service
public class PedidoService {

    private final Map<String, Pedido> pedidos = new ConcurrentHashMap<>();
    private final AtomicLong proximoId = new AtomicLong(1);

    public Pedido criar(BigDecimal valorProdutos, BigDecimal frete) {
        if (valorProdutos == null || valorProdutos.signum() <= 0 || frete == null || frete.signum() < 0) {
            throw new ErroDoPedido(ErroDoPedido.Codigo.PEDIDO_INVALIDO);
        }
        Pedido pedido = new Pedido(String.valueOf(proximoId.getAndIncrement()), valorProdutos, frete);
        pedidos.put(pedido.getId(), pedido);
        return pedido;
    }

    public Pedido buscar(String id) {
        Pedido pedido = pedidos.get(id);
        if (pedido == null) {
            throw new ErroDoPedido(ErroDoPedido.Codigo.PEDIDO_NAO_ENCONTRADO);
        }
        return pedido;
    }

    public Pedido aplicarAcao(String id, String nomeDaAcao) {
        Pedido pedido = buscar(id);
        Acao acao = interpretar(nomeDaAcao);
        if (!pedido.aplicar(acao)) {
            throw new ErroDoPedido(ErroDoPedido.Codigo.ACAO_NAO_PERMITIDA);
        }
        return pedido;
    }

    private Acao interpretar(String nomeDaAcao) {
        try {
            return Acao.valueOf(nomeDaAcao);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new ErroDoPedido(ErroDoPedido.Codigo.ACAO_INVALIDA);
        }
    }
}
