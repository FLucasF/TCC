package com.loja.dominio;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Todos os dados do serviço em memória; o acesso é controlado pelo {@link Armazem}. */
public class Estado {

    public final Map<String, Produto> produtos = new LinkedHashMap<>();
    public final Map<String, Cliente> clientes = new LinkedHashMap<>();
    public final Map<String, Pedido> pedidos = new LinkedHashMap<>();
    public final Map<String, Aviso> avisos = new LinkedHashMap<>();
    private final Map<String, List<Pedido>> pedidosPorCpf = new LinkedHashMap<>();
    private long ultimaSequenciaAviso;

    public void adicionarPedido(Pedido pedido) {
        pedidos.put(pedido.id(), pedido);
        pedidosPorCpf.computeIfAbsent(pedido.cpf(), k -> new ArrayList<>()).add(pedido);
    }

    public List<Pedido> pedidosDoCliente(String cpf) {
        return pedidosPorCpf.getOrDefault(cpf, List.of());
    }

    public void adicionarAviso(Aviso aviso) {
        avisos.put(aviso.id(), aviso);
        ultimaSequenciaAviso = Math.max(ultimaSequenciaAviso, aviso.seq());
    }

    public long proximaSequenciaAviso() {
        return ++ultimaSequenciaAviso;
    }

    public List<Aviso> avisosDoCliente(String cpf) {
        return avisos.values().stream()
                .filter(a -> a.cpf().equals(cpf))
                .sorted(java.util.Comparator.comparingLong(Aviso::seq))
                .toList();
    }
}
