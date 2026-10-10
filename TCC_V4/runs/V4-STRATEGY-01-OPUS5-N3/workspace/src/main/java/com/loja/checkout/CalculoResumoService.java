package com.loja.checkout;

import com.loja.checkout.api.ItemRequisicao;
import com.loja.checkout.api.ResumoRequisicao;
import com.loja.checkout.api.ResumoResposta;
import com.loja.checkout.clube.NivelClube;
import com.loja.checkout.clube.Niveis;
import com.loja.checkout.cupom.ContextoCupom;
import com.loja.checkout.cupom.Cupons;
import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.Erro;
import com.loja.checkout.dominio.ItemPedido;
import com.loja.checkout.dominio.Pedido;
import com.loja.checkout.dominio.PedidoRecusadoException;
import com.loja.checkout.dominio.Regiao;
import com.loja.checkout.entrega.ModalidadeEntrega;
import com.loja.checkout.entrega.Modalidades;
import com.loja.checkout.pagamento.FormaPagamento;
import com.loja.checkout.pagamento.FormasPagamento;
import com.loja.checkout.pagamento.Liquidacao;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * A sequencia do calculo do resumo, igual para todo pedido. O que varia de
 * caso para caso esta nas implementacoes de entrega, cupom, clube e
 * pagamento; aqui so se pergunta a cada uma.
 */
@Service
public class CalculoResumoService {

    private static final int PARCELAS_PADRAO = 1;

    public ResumoResposta calcular(ResumoRequisicao requisicao) {
        Pedido pedido = pedidoValido(requisicao);
        NivelClube nivel = Niveis.exigir(requisicao.nivelClube());
        Regiao regiao = Regiao.exigir(requisicao.regiao());
        ModalidadeEntrega entrega = entregaDisponivel(requisicao.modalidadeEntrega(), pedido);

        BigDecimal subtotalProdutos = pedido.subtotal();
        BigDecimal frete = nivel.frete(entrega, pedido);
        BigDecimal descontoCupom = Cupons.desconto(requisicao.cupom(),
                new ContextoCupom(pedido, subtotalProdutos, frete));
        BigDecimal seguro = regiao.seguro(subtotalProdutos);
        BigDecimal totalPedido = Dinheiro.centavos(
                subtotalProdutos.subtract(descontoCupom).add(frete).add(seguro));

        int parcelas = requisicao.parcelas() == null ? PARCELAS_PADRAO : requisicao.parcelas();
        FormaPagamento pagamento = pagamentoDisponivel(
                requisicao.formaPagamento(), parcelas, totalPedido);
        Liquidacao liquidacao = pagamento.liquidar(totalPedido, parcelas);

        return new ResumoResposta(
                subtotalProdutos,
                descontoCupom,
                frete,
                entrega.prazoDias(),
                seguro,
                Dinheiro.centavos(liquidacao.totalFinal().subtract(totalPedido)),
                liquidacao.totalFinal(),
                parcelas,
                liquidacao.valorParcela(),
                nivel.credito(subtotalProdutos),
                nivel.brinde(subtotalProdutos));
    }

    private Pedido pedidoValido(ResumoRequisicao requisicao) {
        List<ItemRequisicao> itens = requisicao.itens();
        if (itens == null || itens.isEmpty()) {
            throw new PedidoRecusadoException(Erro.PEDIDO_INVALIDO);
        }
        return new Pedido(itens.stream().map(this::itemValido).toList());
    }

    private ItemPedido itemValido(ItemRequisicao item) {
        if (item == null
                || naoEPositivo(item.precoUnitario())
                || naoEPositivo(item.pesoKg())
                || item.quantidade() == null
                || item.quantidade() <= 0) {
            throw new PedidoRecusadoException(Erro.PEDIDO_INVALIDO);
        }
        return new ItemPedido(item.nome(), item.precoUnitario(), item.quantidade(), item.pesoKg());
    }

    private boolean naoEPositivo(BigDecimal valor) {
        return valor == null || valor.signum() <= 0;
    }

    private ModalidadeEntrega entregaDisponivel(String codigo, Pedido pedido) {
        ModalidadeEntrega entrega = Modalidades.exigir(codigo);
        if (!entrega.atende(pedido)) {
            throw new PedidoRecusadoException(Erro.MODALIDADE_INDISPONIVEL);
        }
        return entrega;
    }

    private FormaPagamento pagamentoDisponivel(String codigo, int parcelas, BigDecimal totalPedido) {
        FormaPagamento pagamento = FormasPagamento.exigir(codigo);
        if (!pagamento.permiteParcelas(parcelas)) {
            throw new PedidoRecusadoException(Erro.PARCELAMENTO_INVALIDO);
        }
        if (!pagamento.atende(totalPedido)) {
            throw new PedidoRecusadoException(Erro.FORMA_PAGAMENTO_INDISPONIVEL);
        }
        return pagamento;
    }
}
