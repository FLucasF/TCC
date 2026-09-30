package com.loja.pedidos.dominio;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Repository;

@Repository
public class PedidoRepositorio {

    private final Map<String, Pedido> pedidos = new ConcurrentHashMap<>();
    private final AtomicLong proximoId = new AtomicLong(1);

    public Pedido criar(BigDecimal valorProdutos, BigDecimal frete) {
        String id = String.valueOf(proximoId.getAndIncrement());
        Pedido pedido = new Pedido(id, valorProdutos, frete);
        pedidos.put(id, pedido);
        return pedido;
    }

    public Pedido buscar(String id) {
        Pedido pedido = pedidos.get(id);
        if (pedido == null) {
            throw new PedidoNaoEncontradoException(id);
        }
        return pedido;
    }
}
