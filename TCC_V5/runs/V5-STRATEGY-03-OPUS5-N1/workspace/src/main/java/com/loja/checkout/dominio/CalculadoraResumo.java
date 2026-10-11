package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;

/** Monta o resumo da compra na ordem em que o valor final é calculado. */
@Service
public class CalculadoraResumo {

    private static final int PARCELA_UNICA = 1;

    public ResumoCompra calcular(SolicitacaoResumo solicitacao) {
        Pedido pedido = pedidoDe(solicitacao.itens());
        NivelClube nivel = exigir(NivelClube.de(solicitacao.nivelClube()), CodigoErro.NIVEL_CLUBE_INVALIDO);
        Regiao regiao = exigir(Regiao.de(solicitacao.regiao()), CodigoErro.REGIAO_INVALIDA);
        ModalidadeEntrega modalidade =
                exigir(ModalidadeEntrega.de(solicitacao.modalidadeEntrega()), CodigoErro.MODALIDADE_INVALIDA);
        recusarSe(!modalidade.atende(pedido), CodigoErro.MODALIDADE_INDISPONIVEL);

        BigDecimal subtotalProdutos = pedido.subtotalProdutos();
        BigDecimal frete = nivel.isentaFrete() ? Dinheiro.ZERO : modalidade.frete(pedido);
        BigDecimal descontoCupom = descontoDe(solicitacao.cupom(), new ContextoCupom(pedido, frete));
        BigDecimal seguro = regiao.seguro(subtotalProdutos);
        BigDecimal totalPedido =
                Dinheiro.centavos(subtotalProdutos.subtract(descontoCupom).add(frete).add(seguro));

        FormaPagamento formaPagamento =
                exigir(FormaPagamento.de(solicitacao.formaPagamento()), CodigoErro.FORMA_PAGAMENTO_INVALIDA);
        int parcelas = Objects.requireNonNullElse(solicitacao.parcelas(), PARCELA_UNICA);
        recusarSe(!formaPagamento.aceitaParcelas(parcelas), CodigoErro.PARCELAMENTO_INVALIDO);
        recusarSe(!formaPagamento.atende(totalPedido), CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
        ResultadoPagamento pagamento = formaPagamento.resolver(totalPedido, parcelas);

        return new ResumoCompra(
                subtotalProdutos,
                descontoCupom,
                frete,
                modalidade.prazoDias(),
                seguro,
                Dinheiro.centavos(pagamento.totalFinal().subtract(totalPedido)),
                pagamento.totalFinal(),
                parcelas,
                pagamento.valorParcela(),
                nivel.creditoProximaCompra(subtotalProdutos),
                nivel.brinde(subtotalProdutos));
    }

    private Pedido pedidoDe(List<SolicitacaoResumo.ItemRecebido> recebidos) {
        recusarSe(recebidos == null || recebidos.isEmpty(), CodigoErro.PEDIDO_INVALIDO);
        return new Pedido(recebidos.stream().map(this::itemDe).toList());
    }

    private ItemPedido itemDe(SolicitacaoResumo.ItemRecebido recebido) {
        recusarSe(recebido == null, CodigoErro.PEDIDO_INVALIDO);
        recusarSe(naoPositivo(recebido.precoUnitario()), CodigoErro.PEDIDO_INVALIDO);
        recusarSe(naoPositivo(recebido.pesoKg()), CodigoErro.PEDIDO_INVALIDO);
        recusarSe(recebido.quantidade() == null || recebido.quantidade() <= 0, CodigoErro.PEDIDO_INVALIDO);
        return new ItemPedido(
                recebido.nome(), recebido.precoUnitario(), recebido.quantidade(), recebido.pesoKg());
    }

    private BigDecimal descontoDe(String codigo, ContextoCupom contexto) {
        if (codigo == null) {
            return Dinheiro.ZERO;
        }
        Cupom cupom = exigir(Cupom.de(codigo), CodigoErro.CUPOM_INVALIDO);
        recusarSe(!cupom.aplicavel(contexto), CodigoErro.CUPOM_NAO_APLICAVEL);
        return cupom.desconto(contexto);
    }

    private boolean naoPositivo(BigDecimal valor) {
        return valor == null || valor.compareTo(BigDecimal.ZERO) <= 0;
    }

    private <T> T exigir(Optional<T> opcao, CodigoErro codigo) {
        return opcao.orElseThrow(() -> new PedidoRecusadoException(codigo));
    }

    private void recusarSe(boolean recusado, CodigoErro codigo) {
        if (recusado) {
            throw new PedidoRecusadoException(codigo);
        }
    }
}
