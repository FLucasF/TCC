package com.loja.pedidos.servico;

import com.loja.pedidos.dominio.Acao;
import com.loja.pedidos.dominio.Pedido;
import com.loja.pedidos.erro.AcaoInvalidaException;
import com.loja.pedidos.erro.AcaoNaoPermitidaException;
import com.loja.pedidos.erro.PedidoInvalidoException;
import com.loja.pedidos.erro.PedidoNaoEncontradoException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

@Service
public class PedidoService {

    private final Map<String, Pedido> pedidos = new ConcurrentHashMap<>();
    private final AtomicLong proximoId = new AtomicLong(1);
    private final Transicoes transicoes = new Transicoes();

    public Pedido criar(BigDecimal valorProdutos, BigDecimal frete) {
        if (valorProdutos == null || valorProdutos.signum() <= 0) {
            throw new PedidoInvalidoException();
        }
        if (frete == null || frete.signum() < 0) {
            throw new PedidoInvalidoException();
        }

        String id = String.valueOf(proximoId.getAndIncrement());
        Pedido pedido = new Pedido(id, valorProdutos, frete);
        pedidos.put(id, pedido);
        return pedido;
    }

    public Pedido buscar(String id) {
        Pedido pedido = pedidos.get(id);
        if (pedido == null) {
            throw new PedidoNaoEncontradoException();
        }
        return pedido;
    }

    public Pedido aplicarAcao(String id, String acaoTexto) {
        Pedido pedido = buscar(id);

        Acao acao = converter(acaoTexto);

        Consumer<Pedido> efeito = transicoes.buscar(pedido.getSituacao(), acao);
        if (efeito == null) {
            throw new AcaoNaoPermitidaException();
        }

        efeito.accept(pedido);
        return pedido;
    }

    private Acao converter(String acaoTexto) {
        if (acaoTexto == null) {
            throw new AcaoInvalidaException();
        }
        try {
            return Acao.valueOf(acaoTexto);
        } catch (IllegalArgumentException e) {
            throw new AcaoInvalidaException();
        }
    }
}
