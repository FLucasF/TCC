package com.loja.pedidos.servico;

import com.loja.pedidos.dominio.Acao;
import com.loja.pedidos.dominio.ErroPedido;
import com.loja.pedidos.dominio.Pedido;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;

/** Acompanha cada pedido depois que o cliente finaliza a compra. */
@Service
public class PedidoServico {

    private final PedidoRepositorio repositorio;

    public PedidoServico(PedidoRepositorio repositorio) {
        this.repositorio = repositorio;
    }

    /** Cria o pedido aguardando pagamento. */
    public synchronized Pedido criar(BigDecimal valorProdutos, BigDecimal frete) {
        if (valorProdutos == null || valorProdutos.signum() <= 0 || frete == null || frete.signum() < 0) {
            throw ErroPedido.PEDIDO_INVALIDO.excecao();
        }
        return repositorio.salvar(new Pedido(repositorio.proximoId(), valorProdutos, frete));
    }

    public synchronized Pedido buscar(String id) {
        return repositorio.porId(id).orElseThrow(ErroPedido.PEDIDO_NAO_ENCONTRADO::excecao);
    }

    /**
     * Confere se a acao vale na situacao atual e, quando vale, aplica os seus
     * efeitos. Quando da erro, o pedido nao muda nada.
     */
    public synchronized Pedido aplicarAcao(String id, String nomeDaAcao) {
        Pedido pedido = buscar(id);
        Acao acao = Acao.de(nomeDaAcao).orElseThrow(ErroPedido.ACAO_INVALIDA::excecao);
        pedido.aplicar(acao).orElseThrow(ErroPedido.ACAO_NAO_PERMITIDA::excecao);
        return pedido;
    }
}
