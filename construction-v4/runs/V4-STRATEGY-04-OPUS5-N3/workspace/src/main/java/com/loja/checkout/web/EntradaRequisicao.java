package com.loja.checkout.web;

import com.loja.checkout.comum.Enums;
import com.loja.checkout.dominio.CodigoErro;
import com.loja.checkout.dominio.Cupom;
import com.loja.checkout.dominio.EntradaPedido;
import com.loja.checkout.dominio.FormaPagamento;
import com.loja.checkout.dominio.ItemPedido;
import com.loja.checkout.dominio.ModalidadeEntrega;
import com.loja.checkout.dominio.NivelClube;
import com.loja.checkout.dominio.PedidoRecusadoException;
import com.loja.checkout.dominio.Regiao;
import java.util.List;
import java.util.Optional;

/** Traduz o que o site enviou para o domínio, recusando o pedido quando o dado não serve. */
record EntradaRequisicao(ResumoRequest requisicao) implements EntradaPedido {

    @Override
    public List<ItemPedido> itens() {
        List<ResumoRequest.ItemRequest> recebidos = requisicao.itens();
        if (recebidos == null || recebidos.isEmpty()) {
            throw new PedidoRecusadoException(CodigoErro.PEDIDO_INVALIDO);
        }
        return recebidos.stream().map(EntradaRequisicao::item).toList();
    }

    @Override
    public NivelClube nivelClube() {
        return exigir(NivelClube.class, requisicao.nivelClube(), CodigoErro.NIVEL_CLUBE_INVALIDO);
    }

    @Override
    public Regiao regiao() {
        return exigir(Regiao.class, requisicao.regiao(), CodigoErro.REGIAO_INVALIDA);
    }

    @Override
    public ModalidadeEntrega modalidadeEntrega() {
        return exigir(ModalidadeEntrega.class, requisicao.modalidadeEntrega(), CodigoErro.MODALIDADE_INVALIDA);
    }

    @Override
    public Optional<Cupom> cupom() {
        String codigo = requisicao.cupom();
        if (codigo == null || codigo.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(exigir(Cupom.class, codigo, CodigoErro.CUPOM_INVALIDO));
    }

    @Override
    public FormaPagamento formaPagamento() {
        return exigir(FormaPagamento.class, requisicao.formaPagamento(), CodigoErro.FORMA_PAGAMENTO_INVALIDA);
    }

    @Override
    public int parcelas() {
        return requisicao.parcelas() == null ? 1 : requisicao.parcelas();
    }

    private static ItemPedido item(ResumoRequest.ItemRequest recebido) {
        if (recebido == null) {
            throw new PedidoRecusadoException(CodigoErro.PEDIDO_INVALIDO);
        }
        return ItemPedido.de(recebido.nome(), recebido.precoUnitario(),
                recebido.quantidade(), recebido.pesoKg());
    }

    private static <E extends Enum<E>> E exigir(Class<E> tipo, String codigo, CodigoErro erro) {
        return Enums.buscar(tipo, codigo).orElseThrow(() -> new PedidoRecusadoException(erro));
    }
}
