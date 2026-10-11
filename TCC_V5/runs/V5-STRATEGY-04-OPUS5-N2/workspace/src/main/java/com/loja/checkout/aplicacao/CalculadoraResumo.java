package com.loja.checkout.aplicacao;

import com.loja.checkout.clube.CatalogoNiveisClube;
import com.loja.checkout.clube.NivelClube;
import com.loja.checkout.cupom.CatalogoCupons;
import com.loja.checkout.cupom.ContextoCupom;
import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.Pedido;
import com.loja.checkout.entrega.CatalogoModalidades;
import com.loja.checkout.entrega.ModalidadeEntrega;
import com.loja.checkout.pagamento.CatalogoFormasPagamento;
import com.loja.checkout.pagamento.Cobranca;
import com.loja.checkout.pagamento.FormaPagamento;
import com.loja.checkout.seguro.CalculadoraSeguro;
import com.loja.checkout.seguro.CatalogoRegioes;
import com.loja.checkout.seguro.Regiao;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/** Calcula o resumo da compra na hora de finalizar o pedido. */
@Service
public class CalculadoraResumo {

    private final MontadorDePedido montadorDePedido;
    private final CatalogoNiveisClube niveisClube;
    private final CatalogoRegioes regioes;
    private final CatalogoModalidades modalidades;
    private final CatalogoCupons cupons;
    private final CatalogoFormasPagamento formasPagamento;
    private final CalculadoraSeguro calculadoraSeguro;

    public CalculadoraResumo(MontadorDePedido montadorDePedido,
                             CatalogoNiveisClube niveisClube,
                             CatalogoRegioes regioes,
                             CatalogoModalidades modalidades,
                             CatalogoCupons cupons,
                             CatalogoFormasPagamento formasPagamento,
                             CalculadoraSeguro calculadoraSeguro) {
        this.montadorDePedido = montadorDePedido;
        this.niveisClube = niveisClube;
        this.regioes = regioes;
        this.modalidades = modalidades;
        this.cupons = cupons;
        this.formasPagamento = formasPagamento;
        this.calculadoraSeguro = calculadoraSeguro;
    }

    public ResumoCompra calcular(SolicitacaoResumo solicitacao) {
        Pedido pedido = montadorDePedido.montar(solicitacao.itens());
        NivelClube nivelClube = nivelClube(solicitacao);
        Regiao regiao = regiao(solicitacao);
        ModalidadeEntrega modalidade = modalidade(solicitacao, pedido);

        BigDecimal subtotalProdutos = pedido.subtotalProdutos();
        BigDecimal frete = frete(modalidade, pedido, nivelClube);
        BigDecimal descontoCupom = descontoCupom(solicitacao, pedido, subtotalProdutos, frete);
        BigDecimal seguro = calculadoraSeguro.seguro(regiao, subtotalProdutos);
        BigDecimal totalPedido = Dinheiro.centavos(
                subtotalProdutos.subtract(descontoCupom).add(frete).add(seguro));

        Cobranca cobranca = cobranca(solicitacao, totalPedido);

        return new ResumoCompra(
                subtotalProdutos,
                descontoCupom,
                frete,
                modalidade.prazoEntregaDias(),
                seguro,
                Dinheiro.centavos(cobranca.totalFinal().subtract(totalPedido)),
                cobranca.totalFinal(),
                cobranca.parcelas(),
                cobranca.valorParcela(),
                nivelClube.creditoProximaCompra(subtotalProdutos),
                nivelClube.ganhaBrinde(subtotalProdutos));
    }

    private NivelClube nivelClube(SolicitacaoResumo solicitacao) {
        return niveisClube.buscar(solicitacao.nivelClube())
                .orElseThrow(ErroCheckout.NIVEL_CLUBE_INVALIDO::erro);
    }

    private Regiao regiao(SolicitacaoResumo solicitacao) {
        return regioes.buscar(solicitacao.regiao())
                .orElseThrow(ErroCheckout.REGIAO_INVALIDA::erro);
    }

    private ModalidadeEntrega modalidade(SolicitacaoResumo solicitacao, Pedido pedido) {
        ModalidadeEntrega modalidade = modalidades.buscar(solicitacao.modalidadeEntrega())
                .orElseThrow(ErroCheckout.MODALIDADE_INVALIDA::erro);
        if (!modalidade.atende(pedido)) {
            throw ErroCheckout.MODALIDADE_INDISPONIVEL.erro();
        }
        return modalidade;
    }

    private BigDecimal frete(ModalidadeEntrega modalidade, Pedido pedido, NivelClube nivelClube) {
        return nivelClube.isentaFrete() ? Dinheiro.ZERO : modalidade.frete(pedido);
    }

    private BigDecimal descontoCupom(SolicitacaoResumo solicitacao, Pedido pedido,
                                     BigDecimal subtotalProdutos, BigDecimal frete) {
        String codigo = solicitacao.cupom();
        if (codigo == null || codigo.isBlank()) {
            return Dinheiro.ZERO;
        }
        Cupom cupom = cupons.buscar(codigo).orElseThrow(ErroCheckout.CUPOM_INVALIDO::erro);
        ContextoCupom contexto = new ContextoCupom(pedido, subtotalProdutos, frete);
        if (!cupom.aplicavel(contexto)) {
            throw ErroCheckout.CUPOM_NAO_APLICAVEL.erro();
        }
        return cupom.desconto(contexto);
    }

    private Cobranca cobranca(SolicitacaoResumo solicitacao, BigDecimal totalPedido) {
        FormaPagamento formaPagamento = formasPagamento.buscar(solicitacao.formaPagamento())
                .orElseThrow(ErroCheckout.FORMA_PAGAMENTO_INVALIDA::erro);
        if (!formaPagamento.permiteParcelas(solicitacao.parcelasOuUma())) {
            throw ErroCheckout.PARCELAMENTO_INVALIDO.erro();
        }
        if (!formaPagamento.atende(totalPedido)) {
            throw ErroCheckout.FORMA_PAGAMENTO_INDISPONIVEL.erro();
        }
        return formaPagamento.cobrar(totalPedido, solicitacao.parcelasOuUma());
    }
}
