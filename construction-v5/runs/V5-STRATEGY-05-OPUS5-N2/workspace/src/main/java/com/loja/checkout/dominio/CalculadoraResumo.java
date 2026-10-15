package com.loja.checkout.dominio;

import com.loja.checkout.dominio.clube.NivelClube;
import com.loja.checkout.dominio.cupom.ContextoCupom;
import com.loja.checkout.dominio.cupom.Cupom;
import com.loja.checkout.dominio.entrega.ModalidadeEntrega;
import com.loja.checkout.dominio.pagamento.Cobranca;
import com.loja.checkout.dominio.pagamento.FormaPagamento;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/** Monta o resumo da compra na ordem definida pelo financeiro. */
@Service
public class CalculadoraResumo {

    private final Catalogo<ModalidadeEntrega> modalidades;
    private final Catalogo<Cupom> cupons;
    private final Catalogo<NivelClube> niveis;
    private final Catalogo<FormaPagamento> formasPagamento;

    public CalculadoraResumo(Catalogo<ModalidadeEntrega> modalidades,
                             Catalogo<Cupom> cupons,
                             Catalogo<NivelClube> niveis,
                             Catalogo<FormaPagamento> formasPagamento) {
        this.modalidades = modalidades;
        this.cupons = cupons;
        this.niveis = niveis;
        this.formasPagamento = formasPagamento;
    }

    public ResumoCompra calcular(SolicitacaoResumo solicitacao) {
        Pedido pedido = Pedido.conferido(solicitacao.itens());
        NivelClube nivel = niveis.exigir(solicitacao.nivelClube(), MotivoRecusa.NIVEL_CLUBE_INVALIDO);
        Regiao regiao = Regiao.exigir(solicitacao.regiao());
        ModalidadeEntrega modalidade =
                modalidades.exigir(solicitacao.modalidadeEntrega(), MotivoRecusa.MODALIDADE_INVALIDA);
        if (!modalidade.atende(pedido)) {
            throw new PedidoRecusadoException(MotivoRecusa.MODALIDADE_INDISPONIVEL);
        }

        BigDecimal subtotalProdutos = pedido.subtotalProdutos();
        BigDecimal frete = nivel.freteCobrado(modalidade.frete(pedido));
        BigDecimal descontoCupom = descontoDoCupom(solicitacao.cupom(), new ContextoCupom(pedido, frete));
        BigDecimal seguro = regiao.seguro(subtotalProdutos);
        BigDecimal totalPedido = Dinheiro.centavos(
                subtotalProdutos.subtract(descontoCupom).add(frete).add(seguro));

        Cobranca cobranca = cobrar(solicitacao, totalPedido);

        return new ResumoCompra(
                subtotalProdutos,
                descontoCupom,
                Dinheiro.centavos(frete),
                modalidade.prazoDias(),
                seguro,
                Dinheiro.centavos(cobranca.valorFinal().subtract(totalPedido)),
                cobranca.valorFinal(),
                cobranca.parcelas(),
                cobranca.valorParcela(),
                nivel.credito(subtotalProdutos),
                nivel.brinde(subtotalProdutos));
    }

    private BigDecimal descontoDoCupom(String codigo, ContextoCupom contexto) {
        if (codigo == null) {
            return Dinheiro.ZERO;
        }
        Cupom cupom = cupons.exigir(codigo, MotivoRecusa.CUPOM_INVALIDO);
        if (!cupom.aplicavel(contexto)) {
            throw new PedidoRecusadoException(MotivoRecusa.CUPOM_NAO_APLICAVEL);
        }
        return cupom.desconto(contexto);
    }

    private Cobranca cobrar(SolicitacaoResumo solicitacao, BigDecimal totalPedido) {
        FormaPagamento forma =
                formasPagamento.exigir(solicitacao.formaPagamento(), MotivoRecusa.FORMA_PAGAMENTO_INVALIDA);
        int parcelas = solicitacao.parcelasOuUma();
        if (!forma.aceitaParcelas(parcelas)) {
            throw new PedidoRecusadoException(MotivoRecusa.PARCELAMENTO_INVALIDO);
        }
        if (!forma.atende(totalPedido)) {
            throw new PedidoRecusadoException(MotivoRecusa.FORMA_PAGAMENTO_INDISPONIVEL);
        }
        return forma.cobrar(totalPedido, parcelas);
    }
}
