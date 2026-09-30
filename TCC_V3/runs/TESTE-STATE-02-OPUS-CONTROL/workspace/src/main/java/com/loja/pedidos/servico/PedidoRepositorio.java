package com.loja.pedidos.servico;

import com.loja.pedidos.dominio.Pedido;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Repository;

/** Guarda os pedidos na memoria enquanto o servico estiver rodando. */
@Repository
public class PedidoRepositorio {

    private final Map<String, Pedido> pedidos = new ConcurrentHashMap<>();
    private final AtomicLong proximoId = new AtomicLong(1);

    /** Gera um id novo, diferente para cada pedido. */
    public String proximoId() {
        return String.valueOf(proximoId.getAndIncrement());
    }

    public Pedido salvar(Pedido pedido) {
        pedidos.put(pedido.id(), pedido);
        return pedido;
    }

    public Optional<Pedido> porId(String id) {
        return id == null ? Optional.empty() : Optional.ofNullable(pedidos.get(id));
    }
}
