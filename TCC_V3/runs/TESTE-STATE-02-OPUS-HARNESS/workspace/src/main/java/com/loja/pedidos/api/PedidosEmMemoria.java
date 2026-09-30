package com.loja.pedidos.api;

import com.loja.pedidos.dominio.Pedido;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class PedidosEmMemoria {

    private final Map<String, Pedido> pedidos = new ConcurrentHashMap<>();
    private final AtomicLong proximoId = new AtomicLong(1);

    public Pedido criar(BigDecimal valorProdutos, BigDecimal frete) {
        Pedido pedido = new Pedido(String.valueOf(proximoId.getAndIncrement()), valorProdutos, frete);
        pedidos.put(pedido.id(), pedido);
        return pedido;
    }

    public Optional<Pedido> buscar(String id) {
        return Optional.ofNullable(pedidos.get(id));
    }
}
